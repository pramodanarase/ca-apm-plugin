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
 * A default implementation of ComparisonStrategy This required the output format of Comparison
 * strategies in DefaultStrategyResult format
 *
 * <p>The goal of this comparison-strategy is to compare the current build's performance with a
 * given static threshold value.
 *
 * <p>If current build's metric values go above the threshold value, it will be reported
 *
 * @author Avinash Chandwani
 */
public class StaticThresholdComparisonStrategy
    implements ComparisonStrategy<DefaultStrategyResult> {

  private StrategyConfiguration strategyConfiguration;
  private String comparisonStrategyName;

  public void setConfiguration(StrategyConfiguration strategyConfiguration) {
    this.strategyConfiguration = strategyConfiguration;
    this.comparisonStrategyName = strategyConfiguration.getPropertyValue("name");
  }

  public StrategyResult<DefaultStrategyResult> doCompare(
      BuildInfo benchMarkBuildInfo, BuildInfo currentBuildInfo)
      throws BuildComparatorException, BuildExecutionException {
    JenkinsPlugInLogger.fine("Stall Count Comparison Strategy comparison has been started");
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
        BuildPerformanceData currentBuildPerformanceData =
            MetricDataHelper.getMetricData(agentSpecifier, metricSpecifier, currentBuildInfo);
        Map<String, Double> currentAverageValues =
            FormulaHelper.getAverageValues(currentBuildPerformanceData);
        agentComparisonResult =
            FormulaHelper.thresholdValueBasedCrossBuildMetricPathComparison(
                thresholdValue, currentAverageValues);
        Map<String, List<TimeSliceValue>> currentSliceValues =
            FormulaHelper.getTimeSliceGroupByMetricPath(currentBuildPerformanceData);
        agentComparisonResult.attachEveryPointResult(thresholdValue, currentSliceValues);
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
    JenkinsPlugInLogger.fine(
        "Stall Count Comparison Strategy comparison has been completed successfully");
    return comparisonOutput;
  }
}
