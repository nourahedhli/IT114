public class Recursion {
  
	public static int sum(int num) {
	    
		//if (num > 0) {
			//return num + sum(num - 1);
		//}
		int Sum = 10 ;
		while (num <0  );{
		    num = num + (num - Sum );
		    Sum--;
		}
		return Sum;
	}

	public static void main(String[] args) {
		System.out.println(sum(10));
	}
}
