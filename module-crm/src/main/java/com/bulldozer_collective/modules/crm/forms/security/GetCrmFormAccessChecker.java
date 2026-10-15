package com.bulldozer_collective.modules.crm.forms.security;

import com.bulldozer_collective.modules.crm.security.DefaultAccessChecker;
import com.bulldozer_collective.logging.BulldozerLogger;
import com.bulldozer_collective.security.resolvers.RequestContext;
import com.bulldozer_collective.security.services.SecurityTools;
import org.springframework.stereotype.Service;

@Service
public class GetCrmFormAccessChecker extends DefaultAccessChecker {

    public GetCrmFormAccessChecker(SecurityTools tools, BulldozerLogger logger) {
        super(tools, logger);
    }

    @Override
    public void checkAccess(RequestContext context) {
        super.checkAccess(context);
    }
}
