package neuralcoder3.servermod.network;

import com.google.gson.annotations.SerializedName;

public class PlayerInfo {
    @SerializedName("name")
    public String name;
    
    @SerializedName("x")
    public double x;
    
    @SerializedName("y")
    public double y;
    
    @SerializedName("z")
    public double z;
    
    @SerializedName("dimension")
    public String dimension;
    
    public PlayerInfo() {}
    
    public PlayerInfo(String name, double x, double y, double z, String dimension) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.z = z;
        this.dimension = dimension;
    }
    
    @Override
    public String toString() {
        return String.format("PlayerInfo{name='%s', x=%.2f, y=%.2f, z=%.2f, dimension='%s'}", 
            name, x, y, z, dimension);
    }
}
