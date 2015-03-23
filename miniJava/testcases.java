// fail
class Compiler {


	/**
	 * @param args  if no args provided parse from keyboard input
	 *              else args[0] is name of file containing input to be parsed  
	 */
	public static void main(String[] args) {
		int x = 5;
		int y = 2;
		if (x+y == 7)
			int z = 5;
	}
}

// pass
class Compiler {


	/**
	 * @param args  if no args provided parse from keyboard input
	 *              else args[0] is name of file containing input to be parsed  
	 */
	public static void main(String[] args) {
		int x = 5;
		int y = 2;
		if (x+y == 7)
			x = 6;
	}
}

//pass
class Compiler {


	/**
	 * @param args  if no args provided parse from keyboard input
	 *              else args[0] is name of file containing input to be parsed  
	 */
	public static void main(String[] args) {
		System.out.println(5);
	}
}