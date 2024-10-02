var now = java.time.LocalDateTime.now();

now.toString();
now.minusHours(48);
now.getDayOfYear();

var then = now.minusDays(10);
var thenInstant = then.toInstant(java.time.ZoneOffset.UTC);
var thenDate = new java.util.Date.from(thenInstant);

var nowDate = new java.util.Date();
nowDate.compareTo(thenDate);
thenDate.compareTo(nowDate);
