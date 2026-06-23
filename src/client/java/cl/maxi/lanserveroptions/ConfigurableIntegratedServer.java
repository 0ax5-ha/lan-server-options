package cl.maxi.lanserveroptions;

public interface ConfigurableIntegratedServer {
	void lanServerOptions$setMaxPlayers(int maxPlayers);

	boolean lanServerOptions$refreshLanAnnouncement();
}
