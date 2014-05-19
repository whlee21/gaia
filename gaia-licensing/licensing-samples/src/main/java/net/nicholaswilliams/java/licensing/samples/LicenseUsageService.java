package net.nicholaswilliams.java.licensing.samples;

import net.nicholaswilliams.java.licensing.License;
import net.nicholaswilliams.java.licensing.LicenseManager;
import net.nicholaswilliams.java.licensing.exception.InvalidLicenseException;

public class LicenseUsageService
{
//    ...
    public void useLicense()
    {
//        ...
        LicenseManager manager = LicenseManager.getInstance();
        
        License license = manager.getLicense("client1");
        try {
            manager.validateLicense(license);
        } catch(InvalidLicenseException e) { return; }
        
        int seats = license.getNumberOfLicenses();
        
        boolean bool;
        try {
            bool = manager.hasLicenseForAllFeatures("client2", "feature1", "feature2");
        } catch(InvalidLicenseException e) { bool = false; }
//        ...    
    }
//    ...
}
