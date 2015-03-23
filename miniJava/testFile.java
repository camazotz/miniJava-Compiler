// pass
class Node {
	Node next;
	int data;

	public void setNext(Node x) {
		next = x;
	}

	public void setData(int x) {
		data = x;
	}
}

class Compiler {


	/**
	 * @param args  if no args provided parse from keyboard input
	 *              else args[0] is name of file containing input to be parsed  
	 */
	public static void main(String[] args) {
		Node x = new Node();
		x.setData(4);
	}
}