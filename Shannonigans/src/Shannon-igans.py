#!/usr/bin/env python3

# Copyright (c) 2024 ServiceNow, Inc.

# Permission is hereby granted, free of charge, to any person obtaining a copy
# of this software and associated documentation files (the "Software"), to deal
# in the Software without restriction, including without limitation the rights
# to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
# copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:

# The above copyright notice and this permission notice shall be included in all
# copies or substantial portions of the Software.

# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
# AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
# SOFTWARE.

import argparse
import numpy as np
import matplotlib.pyplot as plt
import ruptures as rpt
from numba import njit
from typing import List
from hilbertcurve.hilbertcurve import HilbertCurve
import umap
from ruptures.base import BaseCost
from sklearn.cluster import KMeans
import pandas as pd
from tabulate import tabulate
from sklearn.metrics import precision_recall_fscore_support
import plotly.graph_objects as go
from plotly.subplots import make_subplots
from sklearn.preprocessing import MinMaxScaler


@njit
def optimized_shannon_entropy(data: np.ndarray) -> float:
    """calculate Shannon entropy of the input data using Numba optimization"""
    counts = np.bincount(data)
    total = data.size
    nonzero_counts = counts[counts > 0]
    probabilities = nonzero_counts / total
    return -np.sum(probabilities * np.log2(probabilities))


class OptimizedShannonCost(BaseCost):
    """custom cost function using optimized Shannon entropy"""
    model = "OptimizedShannon"
    min_size = 2

    def fit(self, signal):
        self.signal = signal
        return self

    def error(self, start, end):
        sub = self.signal[start:end]
        return (end - start) * optimized_shannon_entropy(sub)


class ShannonigansAnalyzer:
    """analyzer class using OptimizedShannonCost for change point detection of binary data"""

    def __init__(self, block_size: int = 256):
        self.block_size = block_size

    @staticmethod
    def analyze_file(filepath: str) -> np.ndarray:
        """reads binary file into a NumPy array"""
        with open(filepath, 'rb') as f:
            return np.frombuffer(f.read(), dtype=np.uint8)

    def detect_change_points(self, data: np.ndarray, n_bkps: int = 10) -> List[int]:
        """detects change points using the Ruptures library"""
        algo = rpt.Dynp(custom_cost=OptimizedShannonCost()).fit(data)
        return algo.predict(n_bkps=n_bkps)

    def visualize_change_points(self, data: np.ndarray, change_points: List[int]):
        """visualizes binary data, and detects change points"""
        fig = go.Figure(
            data=[go.Heatmap(z=data.reshape(1, -1), colorscale='Viridis', showscale=False)]
        )
        for cp in change_points:
            fig.add_vline(x=cp, line_dash="dash", line_color="red")
        fig.update_layout(title='Detected Change Points', width=1000, height=200)
        fig.show()

    @staticmethod
    def hilbert_curve_visualization(data: np.ndarray, p: int = 8, n: int = 2):
        """visualizes data using a Hilbert curve"""
        hilbert_curve = HilbertCurve(p, n)
        coords = [hilbert_curve.point_from_distance(i) for i in range(len(data))]
        coords = np.array(coords)
        fig = go.Figure(
            data=[go.Scatter(
                x=coords[:, 0],
                y=coords[:, 1],
                mode='markers',
                marker=dict(color=data, colorscale='Viridis')
            )]
        )
        fig.update_layout(title='Hilbert Curve Visualization', width=800, height=800)
        fig.show()

    @staticmethod
    def umap_visualization(data: np.ndarray, n_components: int = 2, n_neighbors: int = 15):
        """UMAP visualization of binary data."""
        scaler = MinMaxScaler()
        scaled_data = scaler.fit_transform(data.reshape(-1, 1))
        reducer = umap.UMAP(n_components=n_components, n_neighbors=n_neighbors, metric='manhattan', random_state=42)
        embedding = reducer.fit_transform(scaled_data)
        fig = go.Figure(
            data=[go.Scatter(
                x=embedding[:, 0],
                y=embedding[:, 1],
                mode='markers',
                marker=dict(color=data, colorscale='Viridis')
            )]
        )
        fig.update_layout(title='UMAP Visualization', width=800, height=800)
        fig.show()

    def create_summary_table(self, change_points: List[int], data: np.ndarray, segment_classes: List[int]) -> pd.DataFrame:
        """creates a summary table for detected segments."""
        starts = [0] + change_points[:-1]
        segments = []
        for i, (start, end) in enumerate(zip(starts, change_points)):
            segment_data = data[start:end]
            entropy = optimized_shannon_entropy(segment_data)
            print(f"Segment {i+1}: Start={start}, End={end}, Length={end-start}, Entropy={entropy}")
            segments.append({
                "Segment": i + 1,
                "Start": start,
                "End": end,
                "Length": end - start,
                "Entropy": entropy,
                "Class": segment_classes[i] if i < len(segment_classes) else None
            })
        return pd.DataFrame(segments)

def main():
    parser = argparse.ArgumentParser(
        description="analyzes a binary file, and detects change points using ShannonigansAnalyzer"
    )
    parser.add_argument("file", help="Path to the binary file for analysis")
    parser.add_argument("--n_bkps", type=int, default=5, help="Number of breakpoints to detect (default: 5)")
    parser.add_argument("--hilbert", action="store_true", help="Also show Hilbert curve visualization")
    parser.add_argument("--umap", action="store_true", help="Also show UMAP visualization")
    args = parser.parse_args()

    analyzer = ShannonigansAnalyzer(block_size=256)
    # reads binary data from file
    data = analyzer.analyze_file(args.file)
    
    # detects change points using the user-specified number of breakpoints
    change_points = analyzer.detect_change_points(data, n_bkps=args.n_bkps)
    print("detected change points:", change_points)

    # visualizes change points on a heatmap
    analyzer.visualize_change_points(data, change_points)

    # if the --hilbert flag is specified, displays the Hilbert curve visualization
    if args.hilbert:
        analyzer.hilbert_curve_visualization(data)

    # if the --umap flag is specified, displays the UMAP visualization
    if args.umap:
        analyzer.umap_visualization(data)

    # creates a summary table for the detected segments
    dummy_classes = [0] * len(change_points)
    summary_df = analyzer.create_summary_table(change_points, data, dummy_classes)

    # clusters the segments based on their entropy valuess
    if len(summary_df) >= 2:
        X = summary_df[['Entropy']].values
        kmeans = KMeans(n_clusters=2, random_state=42)
        clusters = kmeans.fit_predict(X)
    else:
        clusters = [0] * len(summary_df)
    
    # updates the summary table with cluster labels
    summary_df['Class'] = clusters

    # prints the summary table
    print("\nSummary Table:")
    print(tabulate(summary_df, headers='keys', tablefmt='psql'))

if __name__ == "__main__":
    main()
