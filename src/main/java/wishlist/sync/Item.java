package wishlist.sync;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

class ItemSource {
	Integer sourceId;
	String sourceType;
}

class BonusSummary {
	List<String> defaultBonusLists;
	List<String> chanceBonusLists;
	List<String> bonusChances;
}

class DamageClass {	
	public Integer min;
	public Integer max;
	public Integer exactMin;
	public Integer exactMax;
}


class WeaponInfo {	
	public DamageClass damage;
	public Integer weaponSpeed;
	public Float dps;
	
}

@JsonIgnoreProperties(ignoreUnknown = true)
public class Item {
	public Integer id;
	public String description;
	public String name;
	public String icon;
	public Boolean stackable;
	public boolean itemBind;
	
	//List<String> bonusStats;
	//List<String> itemSpells;
	
	public Integer buyPrice;
	public Integer itemClass;
	public Integer itemSubClass;
	public Integer containerSlots;
	
	//@JsonIgnore
	public WeaponInfo weaponInfo;
//	public Integer weaponinfoDamageMin;
//	public Integer weaponinfoDamageMax;
//	public Integer weaponinfoDamageExactMin;
//	public Integer weaponinfoDamageExactMax;
//	public Integer weaponinfoWeaponSpeed;
//	public Float weaponinfoDPS;
	public Integer inventoryType;
	public Boolean equippable;
	public Integer itemLevel;
	public Integer maxCount;
	public Integer maxDurability;
	public Integer minFactionId;
	public Integer minReputation;
	public Integer quality;
	public Integer sellPrice;
	public Integer requiredSkill;
	public Integer requiredLevel;
	public Integer requiredSkillRank;
	//ItemSource itemSource;
	public Integer baseArmor;
	public Boolean hasSockets;
	public Boolean isAuctionable;
	public Integer armor;
	public Integer displayInfoId;
	public String nameDescription;
	public String nameDescriptionColor;
	public Boolean upgradable;
	public Boolean heroicTooltip;
	public String context;
	//List<String> bonusLists;
	//List<String> availableContexts;
	//BonusSummary bonusSummary;
	public Integer artifactId;
}
