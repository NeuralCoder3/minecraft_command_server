package neuralcoder3.servermod.network;

import com.google.gson.annotations.SerializedName;

public class BlockInfo {
    @SerializedName("x")
    public int x;
    
    @SerializedName("y")
    public int y;
    
    @SerializedName("z")
    public int z;
    
    @SerializedName("block")
    public String block;
    
    public BlockInfo() {}
    
    public BlockInfo(int x, int y, int z, String block) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.block = block;
    }
    
    @Override
    public String toString() {
        return String.format("BlockInfo{x=%d, y=%d, z=%d, block='%s'}", x, y, z, block);
    }
}
