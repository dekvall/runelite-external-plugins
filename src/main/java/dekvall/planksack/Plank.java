package dekvall.planksack;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.ItemID;

@Getter
@RequiredArgsConstructor
enum Plank
{
	REGULAR(ItemID.PLANK, 29),
	OAK(ItemID.OAK_PLANK, 60),
	TEAK(ItemID.TEAK_PLANK, 90),
	MAHOGANY(ItemID.MAHOGANY_PLANK, 140);

	private final int id;
	private final int xp;

	static Plank of(int id)
	{
		switch (id)
		{
			case ItemID.PLANK:
				return REGULAR;
			case ItemID.OAK_PLANK:
				return OAK;
			case ItemID.TEAK_PLANK:
				return TEAK;
			case ItemID.MAHOGANY_PLANK:
				return MAHOGANY;
			default:
				return null;
		}
	}
}
