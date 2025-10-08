package neuralcoder3.servermod.network;

import com.google.gson.annotations.SerializedName;

public class BlockPlacement {
    @SerializedName("x")
    public int x;
    
    @SerializedName("y")
    public int y;
    
    @SerializedName("z")
    public int z;
    
    @SerializedName("block")
    public String block;
    
    public BlockPlacement() {}
    
    public BlockPlacement(int x, int y, int z, String block) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.block = block;
    }
    
    @Override
    public String toString() {
        return String.format("BlockPlacement{x=%d, y=%d, z=%d, block='%s'}", x, y, z, block);
    }
}
