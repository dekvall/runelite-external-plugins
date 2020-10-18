package dekvall.danceparty;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
enum Move
{
    CHEER(862) {
        @Override
        boolean isEnabled(DancePartyConfig config) {
            return config.cheer();
        }
    },
    DANCE(866) {
        @Override
        boolean isEnabled(DancePartyConfig config) {
            return config.dance();
        }
    },
    JIG(2106) {
        @Override
        boolean isEnabled(DancePartyConfig config) {
            return config.jig();
        }
    },
    SPIN(2107) {
        @Override
        boolean isEnabled(DancePartyConfig config) {
            return config.spin();
        }
    },
    HEADBANG(2108) {
        @Override
        boolean isEnabled(DancePartyConfig config) {
            return config.headbang();
        }
    },
    JUMP_FOR_JOY(2109) {
        @Override
        boolean isEnabled(DancePartyConfig config) {
            return config.jumpForJoy();
        }
    },
    ZOMBIE_DANCE(3543) {
        @Override
        boolean isEnabled(DancePartyConfig config) {
            return config.zombieDance();
        }
    },
    SMOOTH_DANCE(7533) {
        @Override
        boolean isEnabled(DancePartyConfig config) {
            return config.smoothDance();
        }
    },
    CRAZY_DANCE(7537) {
        @Override
        boolean isEnabled(DancePartyConfig config) {
            return config.crazyDance();
        }
    },
    CHICKEN_DANCE(1835) {
        @Override
        boolean isEnabled(DancePartyConfig config) {
            return config.chickenDance();
        }
    },
    AIR_GUITAR(4751, 1239) {
        @Override
        boolean isEnabled(DancePartyConfig config) {
            return config.airGuitar();
        }
    },
    GOBLIN_SALUTE(2128) {
        @Override
        boolean isEnabled(DancePartyConfig config) {
            return config.goblinSalute();
        }
    },

    SIT_UP(874) {
        @Override
        boolean isEnabled(DancePartyConfig config) {
            return config.sitUp();
        }
    },
    PUSH_UP(872) {
        @Override
        boolean isEnabled(DancePartyConfig config) {
            return config.pushUp();
        }
    },
    STAR_JUMP(870) {
        @Override
        boolean isEnabled(DancePartyConfig config) {
            return config.starJump();
        }
    },
    JOG(868) {
        @Override
        boolean isEnabled(DancePartyConfig config) {
            return config.jog();
        }
    },

    GILDED_ALTAR(3705) {
        @Override
        boolean isEnabled(DancePartyConfig config) {
            return config.gildedAltar();
        }
    },
    MAKE_TELETAB(4068) {
        @Override
        boolean isEnabled(DancePartyConfig config) {
            return config.makeTeletab();
        }
    },
    STRING_AMULET(4412) {
        @Override
        boolean isEnabled(DancePartyConfig config) {
            return config.stringAmulet();
        }
    },
    ;

    private int animId;
    private int gfxId;

    abstract boolean isEnabled(DancePartyConfig config);

    Move(int animId)
    {
        this(animId, -1);
    }

}