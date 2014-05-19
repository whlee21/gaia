package gaia.bigdata.api.user;

import gaia.bigdata.api.user.hbase.HBaseUserService;

import gaia.commons.api.APIModule;

public class UserAPIModule extends APIModule {
	protected void defineBindings() {
		bind(UserResource.class).to(UserSR.class);
		bind(UsersResource.class).to(UsersSR.class);
		bind(UserRolesResource.class).to(UserRolesSR.class);
		bind(UserRolesDeletionResource.class).to(UserRolesDeletionSR.class);
		bind(UserService.class).to(HBaseUserService.class);
	}
}
