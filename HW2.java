public class HW2Loop {
	public static void main(String[] args) {
		int[] arr = new int[]{1, 2, 4, 5, 8, 8, 2};
		//an array of numbers

		int count = arr.length;
		System.out.println("The array has " + count +" elements");
		for(int i = 0; i < count; i++){
		    if (arr[i] % 2 == 0 )
			System.out.println(arr[i]); 
			//using the for-loop to loop over each number and then using the if condition to if the number is even or not.
			
		}
	}
}
