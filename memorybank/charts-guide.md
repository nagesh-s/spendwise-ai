dependencies { implementation 'com.github.PhilJay:MPAndroidChart:v3.1.0' }

PieDataSet ds = new PieDataSet(entries, "By Category");
ds.setColors(colors);
pieChart.setData(new PieData(ds));
pieChart.invalidate();
