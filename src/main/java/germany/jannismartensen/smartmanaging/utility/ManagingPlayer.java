package germany.jannismartensen.smartmanaging.utility;

public class ManagingPlayer {

    private final String uuid;
    private final String name;
    private final String password;
    private final String cookie;

    public ManagingPlayer(String name, String uuid, String password, String cookie) {
        this.uuid = uuid;
        this.name = name;
        this.password = password;
        this.cookie = cookie;
    }

    public String getUUID() {return this.uuid;}
    public String getName() {return this.name;}
    public String getPassword() {return this.password;}
    public String getCookie() {return this.cookie;}

}
