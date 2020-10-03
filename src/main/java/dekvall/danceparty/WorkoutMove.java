package dekvall.danceparty;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
enum WorkoutMove implements Move
{
    SIT_UP(874),
    PUSH_UP(872),
    START_JUMP(870),
    JOG(868),
    ;

    private int animId;
}
