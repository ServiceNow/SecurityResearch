import pandas as pd
import itertools
import matplotlib.pyplot as plt
import struct
import ruptures as rpt
from ruptures.base import BaseCost
import numpy as np
import math
from scipy.stats import entropy
import Levenshtein as lev
import matplotlib.pyplot as plt
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.cluster import KMeans

def plot_dict(mydict):
    lists = sorted(mydict.items()) # sorted by key, return a list of tuples
    x, y = zip(*lists) # unpack a list of pairs into two tuples
    plt.plot(x, y)
    plt.show()

# Get the number of unique bytes and their frequencies
def get_bytes_counts(data):
    import numpy as np
    (item, count) = (np.unique(data, return_counts=True))
    return {item[i]:count[i] for i in range(0,len(item))}


# analyse the number of repeated items based on current position
def get_position_dist(data):
    slices={0:0}
    i=current=0
    prev=x=data[0]
    while (i <len(data)):
        x=data[i]
        if x==prev:
            slices[current]+=1  
        else:
            current=i
            slices[current]=1
            prev=x
        i=i+1 
    return slices

# build sliding window [1,2,3,4,5,6,7] => [1,2,3,4],[2,3,4,5],[3,4,5,6],[4,5,6,7]
def sliding_window(data,step):
    addrDict={}
    for x in range(0,step):
        addrDict[x]=[]
    for i in range(0,len(data)):
        addrDict[i%step].append("".join(["%0.2X" % y for y in data[i:i+step]]))
    return addrDict

# Find indexes and build the DF => Gets a dataframe with values incremented based on ind
def find_indexes(addrDict,inc,ind):
    match ind:
        case "little":
              ind="<I"
        case "big":
              ind=">I"
        case _:
              print("specify an indianess as 'little' or 'big'")

    df = pd.DataFrame(columns = ['addrX','Offset X', 'addrY', 'Offset Y', 'Distance'])
    for idx,addrx in enumerate(addrDict[0:2048]):
            val1=int("".join(["%0.2X" % x for x in struct.pack('<I',int(addrx, 16))]),16)
            for y,addry in enumerate(addrDict[idx+1:2048]):
                    idy=y+idx
                    val2=int("".join(["%0.2X" % x for x in struct.pack('<I',int(addry, 16))]),16)
                    if(val2-val1)==inc:
                            df = df._append({'addrX' : hex(val1), 'Offset X' : hex(idx*4),'addrY' : hex(val2), 
                                            'Offset Y':hex(idy*4),
                                            'Distance' : idy-idx}, 
                                        ignore_index = True)
    return df


def shannon_ent(data): # returns the entropy of a block of data (Basic code borrowed from Binwalk)
        '''
        Performs a Shannon entropy analysis on a given block of data.
        '''
        entropy = 0
        data=[x for x in data]
        if data:
            length = len(data)

            seen = dict(((x, 0) for x in range(0, 256)))
            for byte in data:
                seen[byte] += 1

            for x in range(0, 256):
                p_x = float(seen[x]) / length
                if p_x > 0:
                    entropy -= p_x * math.log(p_x, 2)

        return (entropy / 8)

def _shannon_numpy(data):
            A = np.frombuffer(data, dtype=np.uint8)
            pA = np.bincount(A) / len(A)
            entropy = -np.nansum(pA*np.log2(pA))
            return (entropy / 8)

class ShannonCost(BaseCost): # Defines a cost function for ruptures
    model = ""
    min_size = 2

    def fit(self, signal):
        self.signal = signal
        return self

    def error(self, start, end):

        sub = self.signal[start:end]
        return (end-start)*shannon_ent(sub)
    

    
def draw_entropy_seg(data,n_bkps):
     signal = np.array(data)
     algo = rpt.Dynp(custom_cost=ShannonCost()).fit(signal) 
     result = algo.predict(n_bkps=n_bkps)
     rpt.display(signal, result, result,figsize=(20, 6))
     plt.show()


def slice_when(predicate, iterable): # slice an array based on a criteria
    i, x, size = 0, 0, len(iterable)
    while i < size-1:
        if predicate(iterable[i], iterable[i+1]):
            yield iterable[x:i+1]
            x = i + 1
        i += 1
    yield iterable[x:size]

def repeated_pattern_stats(data,_size): # Get number of repeated patterns based on a specific size
    i=0
    pattern_dict={}
    while(i<len(data)):
        pat="".join("%0.2X" % x for x in data[i:i+_size])
        if pat not in pattern_dict:
            j=0
            pattern_dict[pat]=0
            while (j<len(data)):
                temp="".join("%0.2X" % x for x in data[j:j+_size])
                if temp == pat:
                    pattern_dict[temp]=pattern_dict[temp]+1
                j=j+1
        i=i+1
        
    return pattern_dict

# Get longest repeated string in an array
# Simple code borrowed from https://www.geeksforgeeks.org/longest-repeating-and-non-overlapping-substring/
def longestRepeatedSubstring(data):
    strData="".join([str("%0.2X" % x ) for x in data])
    n = len(strData)
    LCSRe = [[0 for x in range(n + 1)]
                for y in range(n + 1)]
 
    res = "" # To store result
    res_length = 0 # To store length of result
 
    # building table in bottom-up manner
    index = 0
    for i in range(1, n + 1):
        for j in range(i + 1, n + 1):
             
            # (j-i) > LCSRe[i-1][j-1] to remove
            # overlapping
            if (strData[i - 1] == strData[j - 1] and
                LCSRe[i - 1][j - 1] < (j - i)):
                LCSRe[i][j] = LCSRe[i - 1][j - 1] + 1
 
                # updating maximum length of the
                # substring and updating the finishing
                # index of the suffix
                if (LCSRe[i][j] > res_length):
                    res_length = LCSRe[i][j]
                    index = max(i, index)
                 
            else:
                LCSRe[i][j] = 0
 
    # If we have non-empty result, then insert
    # all characters from first character to
    # last character of string
    if (res_length > 0):
        for i in range(index - res_length + 1,
                                    index + 1):
            res = res + strData[i - 1]
 
    return res

def segment_data(data,algo="Pelt",pen=20,n_bkps=1):
    signal = np.array(data)
    match algo:
        case "Pelt":
            algo = rpt.Pelt(custom_cost=ShannonCost()).fit(signal)
            result = algo.predict(pen=pen)
        case "Dynp":
            algo = rpt.Dynp(custom_cost=ShannonCost()).fit(signal)
            result = algo.predict(n_bkps=n_bkps)
        case _:
              print("Only Pelt/Dynp added. You can add you own algo if you want")
    
    
    rpt.display(signal, result, result,figsize=(20, 6))
    plt.show()
     
def calculate_entropy(string):
    probabilities = np.array([string.count(c) for c in set(string)]) / len(string)
    return -np.sum(probabilities * np.log2(probabilities + 1e-10))

def calculate_levenshtein_distance(strings):
    n = len(strings)
    distances = np.zeros((n, n))

    for i in range(n):
        for j in range(n):
            distances[i, j] = lev.distance(strings[i], strings[j])
    
    return distances

def detect_outliers(strings):
    # Step 1: Calculate Entropy
    entropies = np.array([calculate_entropy(s) for s in strings])
    mean_entropy = np.mean(entropies)
    std_entropy = np.std(entropies)
    entropy_threshold = mean_entropy + 2 * std_entropy
    outliers_entropy = {i for i, e in enumerate(entropies) if e > entropy_threshold}

    # Step 2: Calculate Levenshtein Distances
    levenshtein_distances = calculate_levenshtein_distance(strings)
    mean_distances = np.mean(levenshtein_distances, axis=1)
    distance_threshold = np.mean(mean_distances) + 2 * np.std(mean_distances)
    outliers_distance = {i for i, d in enumerate(mean_distances) if d > distance_threshold}

    # Step 3: TF-IDF Vectorization and Clustering
    vectorizer = TfidfVectorizer()
    X = vectorizer.fit_transform(strings)
    kmeans = KMeans(n_clusters=2).fit(X)
    distances_to_centroids = kmeans.transform(X)

    # Set threshold based on distances to the nearest centroid
    threshold = np.percentile(distances_to_centroids, 90)
    outliers_clustering = {i for i, dists in enumerate(distances_to_centroids) if dists.min() > threshold}

    # Step 4: Combine Results
    final_outliers = outliers_entropy.union(outliers_distance).union(outliers_clustering)
    
    return final_outliers

def main_detect_outliers(strings, outlier_func, plot=False, block_size=100, max_outliers=10):
    final_outliers = set()

    while True:
        current_outliers = set()

        # Split the strings into blocks and detect outliers
        for i in range(0, len(strings), block_size):
            block = strings[i:i + block_size]
            block_outliers = outlier_func(block)
            current_outliers.update(block_outliers)

        # If we have already found outliers, repeat detection on the current outliers
        if current_outliers:
            detected_outliers = [strings[i] for i in current_outliers]
            if len(current_outliers) > max_outliers:
                strings = detected_outliers  # Narrow down the strings to detected outliers
            else:
                final_outliers = current_outliers
                break
        else:
            break  # No outliers found, exit the loop

    # Print identified outliers
    print("Identified Outliers:", [strings[i].strip() for i in final_outliers])