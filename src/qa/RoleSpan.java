/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package qa;

/**
 *
 * @author samuellouvan
 */
public class RoleSpan {

    private String textSpan;
    private double[] scores;
    private String roleType;

    public RoleSpan(String textSpan, double[] scores, String roleType) {
        this.textSpan = textSpan;
        this.scores = scores;
        this.roleType = roleType;
    }

    // TODO 
    public double getRoleScore() {
        if (roleType.equalsIgnoreCase("T")) {
            return 1.0;
        }
        else if (roleType.equalsIgnoreCase("A0")) {
            return scores[0];
        }
        else if (roleType.equalsIgnoreCase("A1")) {
            return scores[1];
        }
        else
            return scores[2];
        
    }

    public String getTextSpan() {
        return textSpan;
    }

    public void setTextSpan(String textSpan) {
        this.textSpan = textSpan;
    }

    public double[] getScores() {
        return scores;
    }

    public void setScores(double[] scores) {
        this.scores = scores;
    }

    public String getRoleType() {
        return roleType;
    }

    public void setRoleType(String roleType) {
        this.roleType = roleType;
    }

}
