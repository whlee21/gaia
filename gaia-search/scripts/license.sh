#java -cp ../../gaia-licensing/licensing-licensor-ui-cli/target/licensing-licensor-ui-cli-0.1.jar \
#		 net.nicholaswilliams.java.licensing.licensor.interfaces.cli.ConsoleRSAKeyPairGenerator \
#		 -private ./private.key \
#		 -public ./public.key \
#		 -privatePassword private  \
#		 -password public


#java -cp   ../../gaia-licensing/licensing-licensor-ui-cli/target/licensing-licensor-ui-cli-0.1.jar \
#		 net.nicholaswilliams.java.licensing.licensor.interfaces.cli.ConsoleRSAKeyPairGenerator \
#		 -classes \
#		 -public GaiaSearchPublicKeyDataProvider \
#		 -publicPackage gaia.search.server.license \
#		 -password gaiasearchpublic \
#		 -passwordClass GaiaSearchPublicPasswordProvider \
#		 -passwordPackage gaia.search.server.license \
#		 -private GaiaSearchPrivateKeyDataProvide \
#		 -privatePackage gaia.search.server.license \
#		 -privatePassword gaiasearchprivate \
#		 -privatePasswordClass GaiaSearchPrivatePasswordProvider \
#		 -privatePasswordPackage gaia.search.server.license

java -cp ../../gaia-licensing/licensing-licensor-ui-cli/target/licensing-licensor-ui-cli-0.1.jar \
		 net.nicholaswilliams.java.licensing.licensor.interfaces.cli.ConsoleLicenseGenerator \
		 -config config.properties \
		 -license license-archon.properties
