package com.ca.apm.jenkins.performancecomparatorplugin.comparisonstrategy;

import com.ca.apm.jenkins.api.ComparisonStrategy;
import com.ca.apm.jenkins.api.entity.BuildInfo;
import com.ca.apm.jenkins.api.entity.StrategyConfiguration;
import com.ca.apm.jenkins.api.entity.StrategyResult;
import com.ca.apm.jenkins.api.exception.BuildComparatorException;
import com.ca.apm.jenkins.api.exception.BuildExecutionException;
import com.ca.apm.jenkins.core.entity.AgentComparisonResult;
import com.ca.apm.jenkins.core.entity.BuildPerformanceData;
import com.ca.apm.jenkins.core.entity.DefaultStrategyResult;
import com.ca.apm.jenkins.core.entity.TimeSliceValue;
import com.ca.apm.jenkins.core.helper.FormulaHelper;
import com.ca.apm.jenkins.core.helper.MetricDataHelper;
import com.ca.apm.jenkins.core.logging.JenkinsPlugInLogger;
import com.ca.apm.jenkins.core.util.Constants;
import java.util.List;
import java.util.Map;

/**
 * A Default implementation of the comparison-strategy. The main goal of this strategy is to compare
 * the CPU utilization metrics
 *
 * <p>The goal of this comparison-strategy is to compare the CPU Utilization metric of give Business
 * Segment between the current-build and the bench-mark build
 *
 * <p>It takes the average of the CPU utilization metric metric and compares the current-build's
 * metric path value with the bench-mark build's same corresponding value It uses the threshold
 * percentage value from the configuration. If the performance of a metric path is found to be
 * greater than bench-mark build's corresponding metric path value by a given threshold, the
 * metric-path is said be performing slow else performing under threshold limits
 *
 * @author Avinash Chandwani
 */
public class CPUUtilizationComparisonStrategy implements ComparisonStrategy<DefaultStrategyResult> {

  private StrategyConfiguration strategyConfiguration;
  private String comparisonStrategyName;

  public void setConfiguration(StrategyConfiguration comparisonStrategyInfo) {
    this.strategyConfiguration = comparisonStrategyInfo;
    this.comparisonStrategyName = strategyConfiguration.getPropertyValue("name");
  }

  public StrategyResult<DefaultStrategyResult> doCompare(
      BuildInfo benchMarkBuild, BuildInfo currentBuild)
      throws BuildComparatorException, BuildExecutionException {

    JenkinsPlugInLogger.fine("Mean Latency Strategy comparison has been started");
    String threshold =
        strategyConfiguration.getPropertyValue(comparisonStrategyName + "." + Constants.threshold);
    double thresholdValue = Double.parseDouble(threshold);
    String metricSpecifier =
        strategyConfiguration.getPropertyValue(
            comparisonStrategyName + "." + Constants.metricSpecifier);
    List<String> agentSpecifiers = strategyConfiguration.getAgentSpecifiers();

    StrategyResult<DefaultStrategyResult> comparisonOutput =
        new StrategyResult<DefaultStrategyResult>();
    DefaultStrategyResult strategyResult = new DefaultStrategyResult();
    comparisonOutput.setResult(strategyResult);
    boolean isFrequencySet = false;
    for (String agentSpecifier : agentSpecifiers) {
      AgentComparisonResult agentComparisonResult = null;
      try {
        BuildPerformanceData benchMarkPerformanceData =
            MetricDataHelper.getMetricData(agentSpecifier, metricSpecifier, benchMarkBuild);
        BuildPerformanceData currentBuildPerformanceData =
            MetricDataHelper.getMetricData(agentSpecifier, metricSpecifier, currentBuild);
        Map<String, Double> benchMarkAverageValues =
            FormulaHelper.getAverageValues(benchMarkPerformanceData);
        Map<String, Double> currentAverageValues =
            FormulaHelper.getAverageValues(currentBuildPerformanceData);
        agentComparisonResult =
            FormulaHelper.thresholdPercentageBasedCrossBuildMetricPathComparison(
                thresholdValue, benchMarkAverageValues, currentAverageValues);
        Map<String, List<TimeSliceValue>> benchMarkSliceValues =
            FormulaHelper.getTimeSliceGroupByMetricPath(benchMarkPerformanceData);
        Map<String, List<TimeSliceValue>> currentSliceValues =
            FormulaHelper.getTimeSliceGroupByMetricPath(currentBuildPerformanceData);
        agentComparisonResult.attachEveryPointResult(benchMarkSliceValues, currentSliceValues);
        if (!isFrequencySet) {
          if (agentComparisonResult.getSuccessEntries() != null
              || !(agentComparisonResult.getSuccessEntries().isEmpty())) {
            if (agentComparisonResult.getSuccessEntries().get(0).getCurrentBuildTimeSliceValues()
                    != null
                || !(agentComparisonResult
                    .getSuccessEntries()
                    .get(0)
                    .getCurrentBuildTimeSliceValues()
                    .isEmpty())) {
              comparisonOutput.setFrequency(
                  agentComparisonResult
                      .getSuccessEntries()
                      .get(0)
                      .getCurrentBuildTimeSliceValues()
                      .get(0)
                      .getfrequency());
              isFrequencySet = true;
            } else if (agentComparisonResult
                        .getSuccessEntries()
                        .get(0)
                        .getBenchMarkBuildTimeSliceValues()
                    != null
                || !(agentComparisonResult
                    .getSuccessEntries()
                    .get(0)
                    .getBenchMarkBuildTimeSliceValues()
                    .isEmpty())) {
              comparisonOutput.setFrequency(
                  agentComparisonResult
                      .getSuccessEntries()
                      .get(0)
                      .getBenchMarkBuildTimeSliceValues()
                      .get(0)
                      .getfrequency());
              isFrequencySet = true;
            }

          } else if (agentComparisonResult.getSlowEntries() != null
              || !(agentComparisonResult.getSlowEntries().isEmpty())) {
            if (agentComparisonResult.getSlowEntries().get(0).getCurrentBuildTimeSliceValues()
                    != null
                || !(agentComparisonResult
                    .getSlowEntries()
                    .get(0)
                    .getCurrentBuildTimeSliceValues()
                    .isEmpty())) {
              comparisonOutput.setFrequency(
                  agentComparisonResult
                      .getSlowEntries()
                      .get(0)
                      .getCurrentBuildTimeSliceValues()
                      .get(0)
                      .getfrequency());
              isFrequencySet = true;
            } else if (agentComparisonResult
                        .getSlowEntries()
                        .get(0)
                        .getBenchMarkBuildTimeSliceValues()
                    != null
                || !(agentComparisonResult
                    .getSlowEntries()
                    .get(0)
                    .getBenchMarkBuildTimeSliceValues()
                    .isEmpty())) {
              comparisonOutput.setFrequency(
                  agentComparisonResult
                      .getSlowEntries()
                      .get(0)
                      .getBenchMarkBuildTimeSliceValues()
                      .get(0)
                      .getfrequency());
              isFrequencySet = true;
            }
          }
        }
        if (!isFrequencySet) {
          comparisonOutput.setFrequency(
              agentComparisonResult
                  .getSuccessEntries()
                  .get(0)
                  .getCurrentBuildTimeSliceValues()
                  .get(0)
                  .getfrequency());
          isFrequencySet = true;
        }
      } catch (BuildComparatorException e) {
        JenkinsPlugInLogger.severe(
            "An error has occured while collecting performance metrics for "
                + comparisonStrategyName
                + "from APM-> for agentSpecifier="
                + agentSpecifier
                + ",metricSpecifier ="
                + metricSpecifier
                + e.getMessage(),
            e);
        continue;
      }
      strategyResult.addOneResult(agentSpecifier, agentComparisonResult);
    }
    JenkinsPlugInLogger.fine("CPU-Utilization Strategy comparison has been completed successfully");
    return comparisonOutput;
  }
}
