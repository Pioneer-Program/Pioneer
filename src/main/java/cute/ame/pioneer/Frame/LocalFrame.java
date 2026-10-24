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
import java.util.UUID;

public final class LocalFrame
{
    public final int id;

    final FrameBox box = new FrameBox();
    final FrameMotion motion = new FrameMotion();
    final ArrayList<ServerSubLevel> members = new ArrayList<>(4);
    final ArrayList<ServerPlayer> players = new ArrayList<>(2);
    final ArrayList<Member> records = new ArrayList<>(4);

    private final ArrayList<UUID> ids = new ArrayList<>(4);
    private final List<ServerSubLevel> membersView = Collections.unmodifiableList(members);
    private final List<ServerPlayer> playersView = Collections.unmodifiableList(players);
    private final List<Member> recordsView = Collections.unmodifiableList(records);
    private final List<UUID> idsView = Collections.unmodifiableList(ids);

    final int cell;
    double mass;
    @Nullable
    ServerSubLevel anchor;
    double anchorMass;
    String parentName = "";
    boolean fixed;
    boolean auto;
    int absent;
    int dormant;
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

    public int dormantCount()
    {
        return dormant;
    }

    public List<UUID> memberIds()
    {
        return idsView;
    }

    public List<Member> records()
    {
        return recordsView;
    }

    void clearMembers()
    {
        members.clear();
        players.clear();
        box.clear();
        mass = 0.0;
        anchor = null;
        anchorMass = -1.0;
        for (int i = 0, n = records.size(); i < n; ++i) records.get(i).loaded = false;
    }

    boolean addMember(ServerSubLevel subLevel, double subMass)
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

        return see(subLevel.getUniqueId(), subMass, b);
    }

    private boolean see(UUID id, double subMass, BoundingBox3dc b)
    {
        Member m = record(id);
        boolean added = m == null;
        if (added) m = addRecord(id);

        m.loaded = true;
        m.absentSince = -1L;
        FrameBox r = m.box;
        if (!added && m.mass == subMass && r.minX == b.minX() && r.minY == b.minY() && r.minZ == b.minZ() && r.maxX == b.maxX() && r.maxY == b.maxY() && r.maxZ == b.maxZ())
            return false;

        m.mass = subMass;
        r.set(b.minX(), b.minY(), b.minZ(), b.maxX(), b.maxY(), b.maxZ());
        return true;
    }

    @Nullable
    Member record(UUID id)
    {
        for (int i = 0, n = records.size(); i < n; ++i)
        {
            Member m = records.get(i);
            if (m.id.equals(id)) return m;
        }

        return null;
    }

    Member addRecord(UUID id)
    {
        Member m = new Member(id);
        records.add(m);
        ids.add(id);
        return m;
    }

    void removeRecord(int index)
    {
        records.remove(index);
        ids.remove(index);
    }

    void addPlayer(ServerPlayer player)
    {
        players.add(player);
        AABB b = player.getBoundingBox();
        box.include(b.minX, b.minY, b.minZ, b.maxX, b.maxY, b.maxZ);
    }

    public static final class Member
    {
        final UUID id;
        final FrameBox box = new FrameBox();
        double mass;
        boolean loaded;
        long absentSince = -1L;

        Member(UUID id)
        {
            this.id = id;
        }

        public UUID id()
        {
            return id;
        }

        public boolean loaded()
        {
            return loaded;
        }

        public double mass()
        {
            return mass;
        }

        public FrameBox box()
        {
            return box;
        }
    }
}
