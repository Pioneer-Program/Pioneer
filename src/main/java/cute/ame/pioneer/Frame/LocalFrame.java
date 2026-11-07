package cute.ame.pioneer.Frame;

import cute.ame.pioneer.Core.Frame.FrameBox;
import cute.ame.pioneer.Core.Frame.FrameMotion;
import cute.ame.pioneer.Core.Frame.FrameParent;
import cute.ame.pioneer.SkyPlanet.Data.SolarSystemDefinition;
import dev.ryanhcode.sable.companion.math.BoundingBox3dc;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class LocalFrame
{
    public final int id;

    final FrameBox box = new FrameBox();
    final FrameMotion motion = new FrameMotion();
    final ArrayList<ServerSubLevel> members = new ArrayList<>(4);
    final ArrayList<ServerPlayer> players = new ArrayList<>(2);

    private final List<ServerSubLevel> membersView = Collections.unmodifiableList(members);
    private final List<ServerPlayer> playersView = Collections.unmodifiableList(players);

    double mass;
    int cell;
    @Nullable
    ServerSubLevel anchor;
    double anchorMass;
    String parentName = "";
    boolean fixed;
    boolean auto;
    int absent;
    @Nullable
    private FrameParent parent;
    @Nullable
    private SolarSystemDefinition parentSystem;

    LocalFrame(int id, int cell)
    {
        this.id = id;
        this.cell = cell;
    }

    public int cell()
    {
        return cell;
    }

    public FrameMotion motion()
    {
        return motion;
    }

    public String parentName()
    {
        return parentName;
    }

    public boolean fixed()
    {
        return fixed;
    }

    public boolean auto()
    {
        return auto;
    }

    public int absent()
    {
        return absent;
    }

    @Nullable
    public FrameParent parent(@Nullable SolarSystemDefinition system)
    {
        if (parentName.isEmpty()) return null;
        if (system == parentSystem) return parent;

        parentSystem = system;
        parent = system == null ? null : FrameBodies.find(system, parentName);
        return parent;
    }

    void attach(String parentName, boolean fixed)
    {
        this.parentName = parentName;
        this.fixed = fixed && !parentName.isEmpty();
        this.parent = null;
        this.parentSystem = null;
    }

    public int memberCount()
    {
        return members.size();
    }

    public int playerCount()
    {
        return players.size();
    }

    public double mass()
    {
        return mass;
    }

    @Nullable
    public ServerSubLevel anchor()
    {
        return anchor;
    }

    public List<ServerSubLevel> members()
    {
        return membersView;
    }

    public List<ServerPlayer> players()
    {
        return playersView;
    }

    public FrameBox box()
    {
        return box;
    }

    void clearMembers()
    {
        members.clear();
        players.clear();
        box.clear();
        mass = 0.0;
        anchor = null;
        anchorMass = -1.0;
    }

    void addMember(ServerSubLevel subLevel, double subMass)
    {
        members.add(subLevel);
        mass += subMass;
        BoundingBox3dc b = subLevel.boundingBox();
        box.include(b.minX(), b.minY(), b.minZ(), b.maxX(), b.maxY(), b.maxZ());
        if (subMass > anchorMass)
        {
            anchorMass = subMass;
            anchor = subLevel;
        }
    }

    void addPlayer(ServerPlayer player)
    {
        players.add(player);
        AABB b = player.getBoundingBox();
        box.include(b.minX, b.minY, b.minZ, b.maxX, b.maxY, b.maxZ);
    }
}
