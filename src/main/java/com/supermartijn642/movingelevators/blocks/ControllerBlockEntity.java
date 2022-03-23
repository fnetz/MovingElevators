package com.supermartijn642.movingelevators.blocks;

import com.google.gson.JsonParseException;
import com.supermartijn642.movingelevators.MovingElevators;
import com.supermartijn642.movingelevators.elevator.ElevatorGroup;
import com.supermartijn642.movingelevators.elevator.ElevatorGroupCapability;
import net.minecraft.item.DyeColor;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Direction;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.common.util.Constants;

/**
 * Created 3/29/2020 by SuperMartijn642
 */
public class ControllerBlockEntity extends ElevatorInputBlockEntity {

    private boolean initialized = false;
    private Direction facing;
    private String name;
    private DyeColor color = DyeColor.GRAY;
    private boolean showButtons = true;

    private ElevatorGroup group;

    public ControllerBlockEntity(){
        super(MovingElevators.elevator_tile);
    }

    @Override
    public void tick(){
        super.tick();
        if(!this.initialized){
            this.level.getCapability(ElevatorGroupCapability.CAPABILITY).ifPresent(cap -> cap.add(this));
            this.getGroup().updateFloorData(this, this.name, this.color);
            this.initialized = true;
        }
    }

    @Override
    public Direction getFacing(){
        if(this.facing == null)
            this.facing = this.level.getBlockState(worldPosition).getValue(ControllerBlock.FACING);
        return this.facing;
    }

    @Override
    protected CompoundNBT writeData(){
        CompoundNBT compound = super.writeData();
        compound.putBoolean("hasName", this.name != null);
        if(this.name != null)
            compound.putString("name", this.name);
        compound.putInt("color", this.color.getId());
        compound.putBoolean("showButtons", this.showButtons);
        return compound;
    }

    @Override
    protected void readData(CompoundNBT compound){
        super.readData(compound);
        if(compound.contains("hasName", Constants.NBT.TAG_BYTE))
            this.name = compound.getBoolean("hasName") ? compound.getString("name") : null;
        else if(compound.contains("name")){ // For older versions
            try{
                this.name = ITextComponent.Serializer.fromJson(compound.getString("name")).getString(Integer.MAX_VALUE);
            }catch(JsonParseException ignore){
                this.name = compound.getString("name");
            }
        }else
            this.name = null;
        this.color = DyeColor.byId(compound.getInt("color"));
        this.showButtons = !compound.contains("showButtons", Constants.NBT.TAG_BYTE) || compound.getBoolean("showButtons");
    }

    @Override
    public void setRemoved(){
        if(!this.level.isClientSide)
            this.level.getCapability(ElevatorGroupCapability.CAPABILITY).ifPresent(groups -> groups.remove(this));
        super.setRemoved();
    }

    @Override
    public String getFloorName(){
        return this.name;
    }

    public void setFloorName(String name){
        this.name = name;
        this.dataChanged();
        if(this.hasGroup())
            this.getGroup().updateFloorData(this, this.name, this.color);
    }

    public void setDisplayLabelColor(DyeColor color){
        this.color = color;
        this.dataChanged();
        if(this.hasGroup())
            this.getGroup().updateFloorData(this, this.name, this.color);
    }

    @Override
    public DyeColor getDisplayLabelColor(){
        return this.color;
    }

    public boolean shouldShowButtons(){
        return this.showButtons;
    }

    public void toggleShowButtons(){
        this.showButtons = !this.showButtons;
        this.dataChanged();
    }

    @Override
    public ElevatorGroup getGroup(){
        if(this.group == null || !this.group.isValidGroupFor(this))
            this.group = this.level.getCapability(ElevatorGroupCapability.CAPABILITY).resolve().map(groups -> groups.getGroup(this)).orElse(null);
        return group;
    }

    @Override
    public boolean hasGroup(){
        return this.initialized && getGroup() != null;
    }

    @Override
    public int getFloorLevel(){
        return this.worldPosition.getY();
    }
}
