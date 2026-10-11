-- The seeded catalog of service types reaches a screen for the first time in
-- HU-10, and its names and descriptions were seeded without accents.
--
-- 20260911120700_seed_data.sql is not edited: an applied migration never is.
-- The update is keyed on the old name, so it matches nothing once applied and a
-- rebuilt database reaches the same state. name is unique, which is why it is a
-- usable key here and why each statement can only touch one row.

set search_path = public, extensions;

update public.service_types
set name = 'Consulta médica general',
    description = 'Valoración médica en domicilio, anamnesis y examen físico.'
where name = 'Consulta medica general';

update public.service_types
set description = 'Medición de presión arterial, frecuencia cardíaca, temperatura y saturación.'
where name = 'Control de signos vitales';

update public.service_types
set name = 'Aplicación de inyectables',
    description = 'Administración de medicación intramuscular o subcutánea indicada por receta.'
where name = 'Aplicacion de inyectables';

update public.service_types
set name = 'Colocación y control de vía venosa',
    description = 'Canalización de vía periférica y administración de suero o medicación.'
where name = 'Colocacion y control de via venosa';

update public.service_types
set name = 'Curación de heridas',
    description = 'Limpieza, desinfección y cobertura de heridas o úlceras por presión.'
where name = 'Curacion de heridas';

update public.service_types
set description = 'Acompañamiento, higiene, movilización y control de medicación.'
where name = 'Cuidado de adulto mayor';

update public.service_types
set name = 'Terapia física y rehabilitación',
    description = 'Sesión de fisioterapia en domicilio según plan de tratamiento.'
where name = 'Terapia fisica y rehabilitacion';

update public.service_types
set description = 'Extracción de sangre y recolección de muestras para análisis.'
where name = 'Toma de muestras de laboratorio';

update public.service_types
set description = 'Control de embarazo o puerperio y orientación a la madre.'
where name = 'Control prenatal y posparto';

update public.service_types
set description = 'Control de evolución, curación y manejo del dolor tras una cirugía.'
where name = 'Cuidado postoperatorio';

update public.service_types
set description = 'Colocación, cambio o retiro de sonda vesical.'
where name = 'Sondaje vesical';
