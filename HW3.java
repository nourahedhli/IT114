public class Recursion {
  
	public static int sum(int num) {
	    
		//if (num > 0) {
			//return num + sum(num - 1);
		//}
		int x = 10 ; 
		int y = 0; 
		 for (int i = 0; i < num ; i ++){
		     
		     y+=x;
		     x--;
		     
		 }
		return y;
	}

	public static void main(String[] args) {
		System.out.println(sum(10));
	}
}
