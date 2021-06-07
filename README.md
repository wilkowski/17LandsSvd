# 17LandsSvd

Using the 17Lands data set https://www.17lands.com/public_datasets This code sets a random value for each card and then adds up all the values to calculate an expected win chance. Then it adjusts each card based on how much its actual win chance differed from the expected win chance. This first version has a lot of variance in the final results (cards are generally valued -.01 to .03 with a variance of .01). Results are at least within expectations with good cards getting high values and bad cards getting low or negative values so the algorithm is accurate but not very precise. 
