# How to Use the Hilbert Curve Graph to Complement the Shannon Graph

To find the file offset corresponding to a specific point (X: 15, Y: 30) on
the Hilbert curve visualization, you can use the distance_from_point method
provided by the hilbertcurve library. Here's how you can modify the shannon-igans6.py
script to achieve this:

1. First, locate the hilbert_curve_visualization method in the ShannonigansAnalyzer
class:
```python

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
)

```
Next, modify the method to accept the coordinates of the point of interest, as parameters:

```python
@staticmethod
def hilbert_curve_visualization(data: np.ndarray, p: int = 8, n: int = 2, point_of_interest: tuple = None):
    """visualizes the data using a Hilbert curve."""
    hilbert_curve = HilbertCurve(p, n)
    coords = [hilbert_curve.point_from_distance(i) for i in range(len(data))]
    coords = np.array(coords)

    if point_of_interest:
        # calculates the file offset for the point of interest
        x, y = point_of_interest
        file_offset = hilbert_curve.distance_from_point([x, y])
        print(f"file offset for point ({x}, {y}): {file_offset}")

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
```

3. Update the main function to pass the coordinates of the point of interest to the
hilbert_curve_visualization method:

```python
def main():
    # ... (existing code) ...

    # if the --hilbert flag is specified, display the Hilbert curve visualization
    if args.hilbert:
        point_of_interest = (15, 30)  # Example coordinates
        analyzer.hilbert_curve_visualization(data, point_of_interest=point_of_interest)

    # ... (remaining code) ...
```

Now, when you run the script with the --hilbert flag, it will display the Hilbert
curve visualization and print the file offset corresponding to the specified
point of interest (in this case, (X: 15, Y: 30)).

The distance_from_point method of the HilbertCurve class takes the coordinates of
a point and returns the corresponding distance along the Hilbert curve. This
distance represents the file offset in the one-dimensional binary data.
By using this method, you can easily find the file offset for any point of
interest on the Hilbert curve visualization and correlate it with the corresponding
location in the binary file or the Shannon entropy graph.


# How to Basic: What is a Hilbert Curve?

A Hilbert Curve is a continuous fractal space-filling curve. In simpler terms,
it is a one-dimensional curve that visits every point in a two-dimensional
grid without crossing itself. Because of its unique property of preserving
locality (i.e., points that are close along the curve tend to be close in
the 2D space), Hilbert curves are often used in data visualization, image
processing, and even in database indexing.

---

## How the Script Uses a Hilbert Curve

### Mapping 1D Binary Data to 2D Space

The data being analyzed in the script is a one-dimensional array representing
the bytes read from a binary file. The idea is to use the Hilbert curve to map
each byte's index (a one-dimensional position) to a two-dimensional coordinate.
This transformation can reveal spatial patterns in the binary data that might
not be immediately evident when viewing the data as a simple sequence.

### HilbertCurve Library Usage

The script uses the hilbertcurve module to generate a Hilbert curve object.
In our updated code, we create an instance of the Hilbert curve:
```python
   hilbert_curve = HilbertCurve(p, n)
```

Here, p determines the depth or resolution of the curve, and n is typically the
dimension, which in our case is set to 2 (for two-dimensional visualization).

### How to Convert a Distance Number to a Coordinate

The method point_from_distance(i) is used in a list comprehension to convert
each one-dimensional index i from our binary data into a two-dimensional
coordinate. That is, for each index along the Hilbert curve, the point on
that curve is calculated:
```python
   coords = [hilbert_curve.point_from_distance(i) for i in range(len(data))]
```

This produces an array of 2D coordinates corresponding to the position of each byte when the data is laid out along the Hilbert curve.

### Displaying the Visualization:

Once the 2D coordinates are generated, the script uses Plotly to create an interactive scatter plot:
```python
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
```
- Points and Colors: Each point on the plot represents a byte (or data unit) of the binary file. The color parameter is set to the original data value, so you can see variations in the data. The colorscale='Viridis' provides a modern, perceptually uniform color gradient.

- Plotly Interaction: Since Plotly is interactive, you can zoom in and hover over parts of the visualization to better understand any local patterns or anomalies.

### Why Use a Hilbert Curve for Visualization?

### Locality Preservation

One of the key benefits of using a Hilbert curve is that it preserves spatial locality. This means that if two values are close in the one-dimensional data, they are likely to be close in the 2D representation. This characteristic helps in detecting patterns, clusters, or irregularities in the data that may be related to the structure of the binary file.

### Revealing Structural Patterns

When you visualize binary data along a Hilbert curve, you can sometimes reveal repeating patterns, anomalies, or other structural features that could be relevant in applications such as malware analysis, forensic analysis, or data compression research.

### Enhanced Inspection

It provides an alternative perspective compared to traditional linear plots (like the heatmap used for change point visualization). The Hilbert curve transformation might very well reveal hidden correlations, or spatially related structures, in the data which aids further analysis.
