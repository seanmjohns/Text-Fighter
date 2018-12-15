package org.textfighter.item.weapon;

import org.textfighter.item.Item;

public class Weapon extends Item {

    protected int damage;

    protected String[] typeStrings = {"normal"};

    public int getDamage(){ return damage; }
    public void setDamage(int a){ damage=a;}

    public Weapon(int level, int experience, int type, int damage) {
        super(level, experience, type);
        this.damage = damage;
    }

}
