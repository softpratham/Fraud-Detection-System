package com.fraud.rules;

/**
 * Result object returned by a Rule evaluation.
 */
public class RuleResult {
    private final String ruleName;
    private final boolean matched;
    private final int score;      // contribution to total risk
    private final String reason;

    public RuleResult(String ruleName, boolean matched, int score, String reason) {
        this.ruleName = ruleName;
        this.matched = matched;
        this.score = score;
        this.reason = reason;
    }

    public String getRuleName() { return ruleName; }
    public boolean isMatched() { return matched; }
    public int getScore() { return score; }
    public String getReason() { return reason; }

    @Override
    public String toString() {
        return "RuleResult{" +
                "ruleName='" + ruleName + '\'' +
                ", matched=" + matched +
                ", score=" + score +
                ", reason='" + reason + '\'' +
                '}';
    }
}
