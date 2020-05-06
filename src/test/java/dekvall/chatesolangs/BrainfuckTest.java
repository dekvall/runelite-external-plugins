package dekvall.chatesolangs;

import dekvall.chatesolangs.interpreters.Brainfuck;
import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class BrainfuckTest
{
	@Test
	public void testHelloWorld()
	{
		String res = Brainfuck.interpret("+[-[<<[+[--->]-[<<<]]]>>>-]>-.---.>..>.<<<<-.<+.>>>>>.>.<<.<-.");
		assertEquals("hello world", res);
	}

	@Test
	public void aqpw()
	{
		String res = Brainfuck.interpret("----[---->+<]>++.-[-->+<]>.----[->++++<]>+.-[------->++<]>.[-->+++++++<]>.");
		assertEquals("A q p", res);

		res = Brainfuck.interpret("++++[->++++++++<]>.....---[->+++<]>.");
		assertEquals("     W", res);
	}
}