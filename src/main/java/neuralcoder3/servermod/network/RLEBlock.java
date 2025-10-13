package neuralcoder3.servermod.network;

import com.google.gson.annotations.SerializedName;

public class RLEBlock {
    @SerializedName("count")
    public int count;
    
    @SerializedName("block")
    public String block;
    
    public RLEBlock() {}
    
    public RLEBlock(int count, String block) {
        this.count = count;
        this.block = block;
    }
    
    @Override
    public String toString() {
        return String.format("RLEBlock{count=%d, block='%s'}", count, block);
    }
}
