public final class Skill {
    private final String name;
    private final int damage;
    private final String type;
    private final String soundEffect;

    public Skill(String name, int damage, String type, String soundEffect){
        this.name = name;
        this.damage = damage;
        this.type = type;
        this.soundEffect = soundEffect;
    }

    public String getName() {
        return name;
    }

    public int getDamage() {
        return damage;
    }

    public String getType() {
        return type;
    }

    public String getSoundEffect() {
        return soundEffect;
    }
}