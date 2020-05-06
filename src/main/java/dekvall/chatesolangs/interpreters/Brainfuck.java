package dekvall.chatesolangs.interpreters;

public class Brainfuck
{
	private static int MEMORY_LIMIT = 0xffff;

	public static String interpret(String command)
	{
		StringBuilder sb = new StringBuilder();
		byte[] memory = new byte[MEMORY_LIMIT];
		int ptr = 0;  //memory pointer
		int b = 0;
		char[] tokens = command.toCharArray();
		for (int i = 0; i < tokens.length; i++)
		{
			switch(tokens[i])
			{
				case '>':
					// Move the pointer right
					ptr = ptr == MEMORY_LIMIT - 1 ? 0 : ptr + 1;
					break;
				case '<':
					// Move the pointer left
					ptr = ptr == 0 ? MEMORY_LIMIT - 1 : ptr - 1;
					break;
				case '+':
					// Increment current cell
					memory[ptr]++;
					break;
				case '-':
					// Decrement current cell
					memory[ptr]--;
					break;
				case '.':
					// Output current cell
					sb.append((char) memory[ptr]);
					break;
				case ',':
					// Replace value of current cell with input
					// Not implemented
					break;
				case '[':
					// Jump to matching ] if current value is zero
					if (memory[ptr] == 0)
					{
						i++;
						while(b > 0 || tokens[i] != ']')
						{
							if (tokens[i] == '[')
							{
								b++;
							}
							else if (tokens[i] == ']')
							{
								b--;
							}
							i++;
						}
					}
					break;
				case ']':
					// Jump to matching [ if current value is nonzero
					if (memory[ptr] != 0)
					{
						i--;
						while(b > 0 || tokens[i] != '[')
						{
							if(tokens[i] == ']')
							{
								b++;
							}
							else if (tokens[i] == '[')
							{
								b--;
							}
							i--;
						}
						i--;
					}
					break;
			}
		}
		return sb.toString();
	}
}
