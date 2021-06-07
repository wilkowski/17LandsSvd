package svd;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SingularValueDecomposition {
	
	private static String dataFileLocation = "C:/Users/wilko/eclipse-workspace/17LandsSVD/src/data.csv";
	private static int MAX_GAMES = 500000; // set as small number for testing, use very large number to run all games
	private static double TIME_FACTOR = .97d; // number less than 1, the changes made in each iteration decrease by this amount multiplicatively
	private static int MAX_ITERATIONS = 250;
	private static double CUTOFF_POINT = 0.000005d; // stop iterations if average changes are less than this amount
	
	private static class CardStats{
		public String cardName;
		public double currentCardValue;
		public double updateCardValue;
		public int gamesWithCard = 0;
		public double startingVal;
		public CardStats(String cardName) {
			this.cardName = cardName;
			this.currentCardValue = Math.random() / 40.0d;
			this.updateCardValue = currentCardValue;
			this.startingVal = currentCardValue;
			System.out.println("created card " + cardName + " with value " + currentCardValue);
		}
	}

	
	// does not handle double "" as ", I skipped that for simplicity
	// Splits the string on , that aren't within a pair of "
	public static List<String> parseCsvLine(String csvLine) {
		List<String> result = new ArrayList<String>();
		char[] chars = csvLine.toCharArray();
		StringBuffer curVal = new StringBuffer();
		boolean inQuotes = false;
		for (char ch : chars) {
			if(inQuotes) {
				if (ch == '"') {
					inQuotes = false;
				}else {
					curVal.append(ch);
				}
				continue;
			} else {
				if (ch == '"') {
					inQuotes = true;
				} else if (ch == ',') {
					result.add(curVal.toString());
					curVal = new StringBuffer();
				} else {
					curVal.append(ch);
				}
			}
		}
		if (result.size() != 0 || !curVal.toString().equals("")) {
			result.add(curVal.toString());
		}
		return result;
	}
	
	
	
	public static void main(String[] args) {
		System.out.println("running svd");
		int winColumn = -1;
		Integer deckStart = null;
		Integer deckEnd = null;
		Map<Integer,CardStats> allCardStats = new HashMap<Integer,CardStats>();
		
		try (BufferedReader reader = new BufferedReader(new FileReader(dataFileLocation))){
			String line = reader.readLine();
			List<String> splitFirstLine = parseCsvLine(line);
			for (Integer i = 0; i < splitFirstLine.size(); i++) {
				String entry = splitFirstLine.get(i);
//				System.out.println(entry);
				entry = entry.replace("\"", ""); // remove quotes
				if (entry.equals("won")) {
					winColumn = i;
					System.out.println("won column is " + i);
				}
				if (entry.startsWith("deck_")) {
					String cardName = entry.substring(5);
					allCardStats.put(i, new CardStats(cardName));
					if (deckStart == null) {
						deckStart = i;
					}
					deckEnd = i;
				}
				
//				Thread.sleep(10); // handy for slightly slower printing
			}
			line = reader.readLine();
			
			int gameCount = 0;
			
			// the first loop through just counts how many games each card was used in
			while (line != null) {
//				System.out.println("reading line " + count);
				String [] splitLine = line.split(",");
//				System.out.println(line);
				for (Integer i = deckStart; i<= deckEnd; i++) {
					String cardCountString = splitLine[i];
					if (cardCountString.equals("0")) {
						continue;
					}					
					CardStats iStats = allCardStats.get(i);
					iStats.gamesWithCard++;
				}			
				gameCount++;
				if (gameCount > MAX_GAMES) {
					break;
				}
				line = reader.readLine();
			}
			if (gameCount <= MAX_GAMES) {
				MAX_GAMES = gameCount;
				System.out.println("hit end of file with " + gameCount + " games");
			}
		} catch (Exception e) {
			e.printStackTrace();
		} 
		
		
		
		// now loop throught the file repeatedly to update the values
		for (int iteration = 0; iteration < MAX_ITERATIONS; iteration++) {
			double iterationMultiplier = Math.pow(TIME_FACTOR, iteration); // so later rounds are weighted less
			// file is very big so we just make a new reader each loop
			try (BufferedReader reader = new BufferedReader(new FileReader(dataFileLocation))){
				String line = reader.readLine(); // first line again, we'll skip it this time
				line = reader.readLine();
				int gameCount = 0;
				while (line != null) {
	//				System.out.println("reading line " + count);
					
					
					String [] splitLine = line.split(",");
//					System.out.println(line);
					
					double gameResult = (splitLine[winColumn].equals("True")) ? 1.0d : 0.0d;
					
					double expectedResult = 0.0d;
					
					for (Integer i = deckStart; i<= deckEnd; i++) {
						
						String cardCountString = splitLine[i];
						if (cardCountString.equals("0")) {
							continue;
						}
						Integer cardCount = Integer.valueOf(cardCountString);
						CardStats iStats = allCardStats.get(i);
						expectedResult += iStats.currentCardValue * cardCount; // each card contributes some value to the game
					}
					expectedResult = Math.min(1, Math.max(0, expectedResult));
					
					
//					System.out.println("game was " + gameResult + " expected was " + expectedResult);
					double gameResultDelta = gameResult - expectedResult;
					
					double adjustmentVal = gameResultDelta / (MAX_GAMES)* iterationMultiplier;
					
					for (Integer i = deckStart; i<= deckEnd; i++) {
						Integer cardCount = Integer.valueOf(splitLine[i]);
						if (cardCount == 0) {
							continue;
						}
						CardStats iStats = allCardStats.get(i);
						iStats.updateCardValue += adjustmentVal * cardCount;
						
					}
					gameCount++;
					if (gameCount > MAX_GAMES) {
						break;
					}
					line = reader.readLine();
				}
			} catch (Exception e) {
				e.printStackTrace();
			} 
			
			// now update all the stats and reset for the next iteration
//			int testCount = 0;
			double totalChange = 0.0d;
			for (CardStats stats : allCardStats.values()) {
				
//				if (testCount < 5) {
//					System.out.println("updating " + stats.cardName + " ( " + stats.gamesWithCard + " games) from " + stats.currentCardValue + " to " + stats.updateCardValue);
//				}
				// update the value of the card as calculated
				totalChange += (stats.updateCardValue > stats.currentCardValue ? stats.updateCardValue - stats.currentCardValue : stats.currentCardValue - stats.updateCardValue);
				stats.currentCardValue = stats.updateCardValue;
//				testCount++;
				
			}
			System.out.println("finished iteration " + (iteration +1) + "/" + MAX_ITERATIONS + " average change of " + totalChange/allCardStats.size());
			if (totalChange/allCardStats.size() < CUTOFF_POINT) {
				System.out.println("breaking early due to fixed position");
				break;
			}
		}
		
		
		for (CardStats stats : allCardStats.values()) {
			String cardStats = String.format("%s (%d games) scored %,.5f starting from %,.5f", stats.cardName, stats.gamesWithCard, stats.currentCardValue, stats.startingVal);
			System.out.println(cardStats);
		}
		
		
		System.out.println("ran svd");
	}
	
}
