package org.example.finalprojs.model;

import jakarta.persistence.*;

@Entity
@Table(name = "redeem_items")
public class RedeemItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Subject e.g. "ITIM 2"
    private String subject;

    // NEW: Section e.g. "CP" — only students in this section can claim this reward
    private String section;

    // Display name e.g. "5 Bonus Unit Test Points"
    private String rewardName;

    // Assessment column to update e.g. "unitTest1"
    private String targetAssessment;

    // Points added to the assessment on redemption
    private int pointsAwarded;

    // Cost in student points to claim this reward
    private int cost;

    public RedeemItem() {}

    public RedeemItem(String subject, String section, String rewardName,
                      String targetAssessment, int pointsAwarded, int cost) {
        this.subject          = subject;
        this.section          = section;
        this.rewardName       = rewardName;
        this.targetAssessment = targetAssessment;
        this.pointsAwarded    = pointsAwarded;
        this.cost             = cost;
    }

    public Long getId()                             { return id; }
    public void setId(Long id)                      { this.id = id; }

    public String getSubject()                      { return subject; }
    public void setSubject(String subject)          { this.subject = subject; }

    public String getSection()                      { return section; }
    public void setSection(String section)          { this.section = section; }

    public String getRewardName()                   { return rewardName; }
    public void setRewardName(String rewardName)    { this.rewardName = rewardName; }

    public String getTargetAssessment()             { return targetAssessment; }
    public void setTargetAssessment(String t)       { this.targetAssessment = t; }

    public int getPointsAwarded()                   { return pointsAwarded; }
    public void setPointsAwarded(int pointsAwarded) { this.pointsAwarded = pointsAwarded; }

    public int getCost()                            { return cost; }
    public void setCost(int cost)                   { this.cost = cost; }
}