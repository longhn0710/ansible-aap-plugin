package org.jenkinsci.plugins.ansible_aap.util;

import hudson.model.Item;
import jenkins.model.Jenkins;

public final class DescriptorPermission {
    private DescriptorPermission() {
    }

    public static void checkConfigurePermission(Item item) {
        if (item == null) {
            Jenkins.get().checkPermission(Jenkins.ADMINISTER);
        } else {
            item.checkPermission(Item.CONFIGURE);
        }
    }
}
