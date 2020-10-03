package dekvall.danceparty;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
enum DanceMove implements Move
{
    DANCE(866),
    JIG(2106),
    SPIN(2107),
    HEADBANG(2108),
    ZOMBIE_DANCE(3543),
    SMOOTH_DANCE(7533),
    CRAZY_DANCE(7537),
    JUMP_FOR_JOY(2109),
    ;

    private int animId;
}
