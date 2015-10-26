/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package qa.extractor;

import qa.dep.DependencyNode;

/**
 *
 * @author slouvan
 */
public class RoleSpan {
    String roleLabel;
    DependencyNode nodeSpan;

    public RoleSpan(String roleLabel, DependencyNode nodeSpan) {
        this.roleLabel = roleLabel;
        this.nodeSpan = nodeSpan;
    }

    
    public String getRoleLabel() {
        return roleLabel;
    }

    public void setRoleLabel(String roleLabel) {
        this.roleLabel = roleLabel;
    }

    public DependencyNode getNodeSpan() {
        return nodeSpan;
    }

    public void setNodeSpan(DependencyNode nodeSpan) {
        this.nodeSpan = nodeSpan;
    }

    
}
