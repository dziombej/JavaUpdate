-- Lekcja 5 - dane startowe. Uruchamiane po utworzeniu tabel przez Hibernate
-- (spring.jpa.defer-datasource-initialization=true).
insert into miejsce (nazwa, kategoria, miasto, szerokosc, dlugosc, utworzono) values
  ('Muzeum Narodowe',          'MUZEUM',       'Krakow',   50.0605, 19.9235, current_timestamp),
  ('Muzeum Powstania',         'MUZEUM',       'Warszawa', 52.2323, 20.9809, current_timestamp),
  ('Centrum Nauki Kopernik',   'MUZEUM',       'Warszawa', 52.2419, 21.0285, current_timestamp),
  ('Hotel Pod Roza',           'HOTEL',        'Krakow',   50.0631, 19.9381, current_timestamp),
  ('Hotel Bristol',            'HOTEL',        'Warszawa', 52.2427, 21.0153, current_timestamp),
  ('Pierogarnia Na Rogu',      'RESTAURACJA',  'Krakow',   50.0619, 19.9369, current_timestamp),
  ('Bar Mleczny Prasowy',      'RESTAURACJA',  'Warszawa', 52.2216, 21.0175, current_timestamp),
  ('Restauracja Gdanska',      'RESTAURACJA',  'Gdansk',   54.3498, 18.6530, current_timestamp),
  ('Stacja przy A1',           'STACJA_PALIW', 'Torun',    53.0311, 18.6390, current_timestamp),
  ('Stacja Obwodnica',         'STACJA_PALIW', 'Lodz',     51.7350, 19.4200, current_timestamp),
  ('Parking Wawel',            'PARKING',      'Krakow',   50.0530, 19.9350, current_timestamp),
  ('Parking Dworzec Centralny','PARKING',      'Warszawa', 52.2289, 21.0030, current_timestamp);
