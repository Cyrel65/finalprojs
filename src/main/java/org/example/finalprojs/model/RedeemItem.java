package org.example.finalprojs.model;

import jakarta.persistence.*;

@Entity
@Table(name = "redeem_items")
public class RedeemItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The name of the subject this reward applies to (e.g., "CP 1")
    private String subject;

    // The name of the reward shown to the student (e.g., "5 Bonus Unit Test Points")
    private String rewardName;

    // The assessment column to update in the GradeReport (e.g., "unitTest1", "selfCheck3")
    private String targetAssessment;

    // The fixed number of points to add to the targetAssessment upon redemption
    private int pointsAwarded;

    // The cost of the reward in student 'currentPoints' (from the old Box/Points system)
    private int cost;

    // Default Constructor (JPA)
    public RedeemItem() {}

    // Constructor (Optional, depends on how admin populates)
    public RedeemItem(String subject, String rewardName, String targetAssessment, int pointsAwarded, int cost) {
        this.subject = subject;
        this.rewardName = rewardName;
        this.targetAssessment = targetAssessment;
        this.pointsAwarded = pointsAwarded;
        this.cost = cost;
    }

    // --- Getters and Setters (MUST BE GENERATED) ---
    // (Generate all getters and setters for the above fields)

    // Example Getters:
    public Long getId() { return id; }
    public String getSubject() { return subject; }
    public String getRewardName() { return rewardName; }
    public String getTargetAssessment() { return targetAssessment; }
    public int getPointsAwarded() { return pointsAwarded; }
    public int getCost() { return cost; }
    // ... all setters ...

    public void setId(Long id) { this.id = id; }
    public void setSubject(String subject) { this.subject = subject; }
    public void setRewardName(String rewardName) { this.rewardName = rewardName; }
    public void setTargetAssessment(String targetAssessment) { this.targetAssessment = targetAssessment; }
    public void setPointsAwarded(int pointsAwarded) { this.pointsAwarded = pointsAwarded; }
    public void setCost(int cost) { this.cost = cost; }
}