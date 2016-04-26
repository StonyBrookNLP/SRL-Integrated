/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sbu.srl.googlesent;

import java.util.Objects;
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

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 11 * hash + Objects.hashCode(this.nodeSpan);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final RoleSpan other = (RoleSpan) obj;
        
        return this.nodeSpan.getId() == other.nodeSpan.getId();
       
    }

    public DependencyNode getNodeSpan() {
        return nodeSpan;
    }

    public void setNodeSpan(DependencyNode nodeSpan) {
        this.nodeSpan = nodeSpan;
    }

    
    
}
