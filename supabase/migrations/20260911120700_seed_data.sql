-- Initial catalog of home care service types (RF-05.1).
--
-- Reference data, not test data: the catalog has to exist in every environment,
-- which is why it travels as a migration and not as a local seed file.
-- Prices are in bolivianos and are a reference, not a fixed tariff: the amount
-- of each service is agreed through the offer thread.

insert into public.service_types (name, description, reference_price_bob, estimated_duration_min)
values
    ('Consulta medica general',
     'Valoracion medica en domicilio, anamnesis y examen fisico.',
     150.00, 45),
    ('Control de signos vitales',
     'Medicion de presion arterial, frecuencia cardiaca, temperatura y saturacion.',
     60.00, 20),
    ('Aplicacion de inyectables',
     'Administracion de medicacion intramuscular o subcutanea indicada por receta.',
     50.00, 15),
    ('Colocacion y control de via venosa',
     'Canalizacion de via periferica y administracion de suero o medicacion.',
     120.00, 60),
    ('Curacion de heridas',
     'Limpieza, desinfeccion y cobertura de heridas o ulceras por presion.',
     100.00, 40),
    ('Cuidado de adulto mayor',
     'Acompanamiento, higiene, movilizacion y control de medicacion.',
     200.00, 240),
    ('Terapia fisica y rehabilitacion',
     'Sesion de fisioterapia en domicilio segun plan de tratamiento.',
     130.00, 60),
    ('Terapia respiratoria',
     'Nebulizaciones, ejercicios respiratorios y manejo de secreciones.',
     110.00, 45),
    ('Toma de muestras de laboratorio',
     'Extraccion de sangre y recoleccion de muestras para analisis.',
     80.00, 20),
    ('Control prenatal y posparto',
     'Control de embarazo o puerperio y orientacion a la madre.',
     160.00, 45),
    ('Cuidado postoperatorio',
     'Control de evolucion, curacion y manejo del dolor tras una cirugia.',
     180.00, 60),
    ('Sondaje vesical',
     'Colocacion, cambio o retiro de sonda vesical.',
     140.00, 30)
on conflict (name) do nothing;
