-- =====================================================================
-- HostOps - Sistema de Gestión Hotelera
-- Script de poblado (datos de ejemplo) para PostgreSQL
-- =====================================================================
-- REQUISITOS
--   * Ejecutar DESPUÉS de hostops_schema.sql, sobre una base recién creada.
--   * El script se detiene si la base ya contiene datos o si los canales
--     iniciales no coinciden con lo esperado (ver sección 0).
--
-- CONTENIDO
--   0. Verificaciones previas
--   1. Usuarios (administrador y recepcionistas)
--   2. Habitaciones
--   3. Bloqueos de habitación (mantenimiento)
--   4. Huéspedes
--   5. Reservas
--   6. Detalles de reserva
--   7. Huéspedes alojados por detalle
--   8. Cancelaciones
--   9. Pagos
--  10. Ajuste de secuencias
--  11. Consultas de verificación (comentadas)
--
-- CONVENCIONES DE LOS DATOS
--   * Todos los datos son ficticios. Los correos usan dominios reservados
--     (example.com / example.org) para no apuntar a direcciones reales.
--   * Fecha de referencia del escenario: 18 de octubre de 2026.
--   * Montos en soles (PEN). Hora de Lima (UTC-05).
--   * Tarifas por noche: las reservas con ingreso hasta el 30/09/2026
--     conservan la tarifa anterior (por eso tarifa_noche puede ser menor
--     que habitacion.tarifa); desde el 01/10/2026 rigen las tarifas actuales.
--   * Contraseña de demostración de los tres usuarios: HostOps2026
--     (se guarda solo su hash bcrypt).
--   * Se usan identificadores explícitos (OVERRIDING SYSTEM VALUE) para poder
--     referenciar filas con claridad; al final se ajustan las secuencias.
--   * Canales (creados en el script de esquema): 1 = presencial,
--     2 = telefono, 3 = whatsapp.
-- =====================================================================

BEGIN;

-- ---------------------------------------------------------------------
-- 0. VERIFICACIONES PREVIAS
-- ---------------------------------------------------------------------
DO $$
BEGIN
    -- La base debe estar vacía de datos operativos
    IF EXISTS (SELECT 1 FROM usuario)
       OR EXISTS (SELECT 1 FROM habitacion)
       OR EXISTS (SELECT 1 FROM huesped)
       OR EXISTS (SELECT 1 FROM reserva) THEN
        RAISE EXCEPTION 'La base ya contiene datos. Ejecute este script sobre una base recién creada.';
    END IF;

    -- Los ids de canal usados en las reservas dependen de este orden
    IF (SELECT array_agg(nombre::text ORDER BY id_canal) FROM canal)
       IS DISTINCT FROM ARRAY['presencial', 'telefono', 'whatsapp'] THEN
        RAISE EXCEPTION 'Los canales iniciales no coinciden (se esperaba: presencial, telefono, whatsapp).';
    END IF;

    -- Deben existir los roles creados por el script de esquema
    IF (SELECT count(*) FROM rol WHERE nombre IN ('administrador', 'recepcionista')) <> 2 THEN
        RAISE EXCEPTION 'Faltan los roles administrador y recepcionista.';
    END IF;
END;
$$;


-- ---------------------------------------------------------------------
-- 1. USUARIOS
-- ---------------------------------------------------------------------
INSERT INTO usuario
    (id_usuario, id_rol, nombre_usuario, nombre_completo, correo, contrasena_hash, activo)
OVERRIDING SYSTEM VALUE
VALUES
    (1, (SELECT id_rol FROM rol WHERE nombre = 'administrador'),
        'jvidaurre', 'Javier Vidaurre Almeyda', 'javier.vidaurre@hostops.example.com',
        '$2b$12$s1beiJCzbT6UzggBp/CuJeuehH8mLyvsl8Emv3yioJNc96F6.AZzi', TRUE),
    (2, (SELECT id_rol FROM rol WHERE nombre = 'recepcionista'),
        'rsantos', 'Ricardo Santos Ortega', 'ricardo.santos@hostops.example.com',
        '$2b$12$s1beiJCzbT6UzggBp/CuJeuehH8mLyvsl8Emv3yioJNc96F6.AZzi', TRUE),
    (3, (SELECT id_rol FROM rol WHERE nombre = 'recepcionista'),
        'pcarrasco', 'Pablo Carrasco Ferrer', 'pablo.carrasco@hostops.example.com',
        '$2b$12$s1beiJCzbT6UzggBp/CuJeuehH8mLyvsl8Emv3yioJNc96F6.AZzi', TRUE);


-- ---------------------------------------------------------------------
-- 2. HABITACIONES (18 habitaciones en 4 pisos)
--    El estado refleja la situación al 18/10/2026:
--      ocupada      -> 102, 104, 201, 202, 303, 305 (huéspedes alojados)
--      limpieza     -> 101 (check-out de hoy)
--      mantenimiento-> 302 (bloqueo vigente)
-- ---------------------------------------------------------------------
INSERT INTO habitacion
    (id_habitacion, numero, tipo, capacidad, tarifa, estado)
OVERRIDING SYSTEM VALUE
VALUES
    ( 1, '101', 'simple',      1,  90.00, 'limpieza'),
    ( 2, '102', 'simple',      1,  90.00, 'ocupada'),
    ( 3, '103', 'doble',       2, 140.00, 'disponible'),
    ( 4, '104', 'doble',       2, 140.00, 'ocupada'),
    ( 5, '105', 'matrimonial', 2, 160.00, 'disponible'),
    ( 6, '201', 'simple',      1,  90.00, 'ocupada'),
    ( 7, '202', 'doble',       2, 140.00, 'ocupada'),
    ( 8, '203', 'doble',       2, 140.00, 'disponible'),
    ( 9, '204', 'matrimonial', 2, 160.00, 'disponible'),
    (10, '205', 'matrimonial', 2, 160.00, 'disponible'),
    (11, '206', 'triple',      3, 200.00, 'disponible'),
    (12, '301', 'doble',       2, 150.00, 'disponible'),
    (13, '302', 'matrimonial', 2, 170.00, 'mantenimiento'),
    (14, '303', 'triple',      3, 210.00, 'ocupada'),
    (15, '304', 'triple',      3, 210.00, 'disponible'),
    (16, '305', 'suite',       4, 320.00, 'ocupada'),
    (17, '401', 'suite',       4, 340.00, 'disponible'),
    (18, '402', 'suite',       4, 360.00, 'disponible');


-- ---------------------------------------------------------------------
-- 3. BLOQUEOS DE HABITACIÓN (rangos inclusivos en ambos extremos)
--    Se insertan antes que las reservas; ninguna reserva activa los cruza.
-- ---------------------------------------------------------------------
INSERT INTO bloqueo_habitacion
    (id_bloqueo, id_habitacion, fecha_inicio, fecha_fin, motivo)
OVERRIDING SYSTEM VALUE
VALUES
    (1,  2, '2026-08-24', '2026-08-25', 'Fumigación preventiva de la habitación'),
    (2,  9, '2026-09-14', '2026-09-18', 'Pintura y resane de paredes'),
    (3, 13, '2026-10-12', '2026-10-20', 'Reparación del sistema de aire acondicionado'),
    (4,  5, '2026-11-09', '2026-11-10', 'Cambio de colchón y revisión de gasfitería'),
    (5, 17, '2026-12-01', '2026-12-05', 'Renovación del mobiliario de la suite');


-- ---------------------------------------------------------------------
-- 4. HUÉSPEDES (37)
--    1-24  : peruanos (DNI)
--    25-37 : extranjeros (pasaporte; el 36 tiene carné de extranjería)
-- ---------------------------------------------------------------------
INSERT INTO huesped
    (id_huesped, tipo_documento, numero_documento, nombres, apellidos,
     telefono, correo, nacionalidad, fecha_nacimiento, preferencias)
OVERRIDING SYSTEM VALUE
VALUES
    -- Huéspedes de Perú
    ( 1, 'DNI', '45872316', 'Carlos Alberto',      'Mendoza Rojas',
         '987412365', 'carlos.mendoza@example.com',      'Peruana', '1984-03-12',
         'Habitación en piso alto y silenciosa'),
    ( 2, 'DNI', '70418293', 'María Fernanda',      'Torres Salazar',
         '956231874', 'maria.torres@example.com',        'Peruana', '1991-07-25',
         'Prefiere almohadas firmes'),
    ( 3, 'DNI', '09837412', 'Luis Miguel',         'Vargas Paredes',
         '945871203', 'luis.vargas@example.com',         'Peruana', '1978-11-02', NULL),
    ( 4, 'DNI', '46201875', 'Ana Lucía',           'Gutiérrez Medina',
         '991204586', 'ana.gutierrez@example.com',       'Peruana', '1989-05-19',
         'Solicita check-in temprano cuando hay disponibilidad'),
    ( 5, 'DNI', '07219384', 'José Antonio',        'Ramírez Castillo',
         '963512078', NULL,                              'Peruana', '1972-09-30', NULL),
    ( 6, 'DNI', '10458263', 'Rosa Elena',          'Flores Delgado',
         '974120356', 'rosa.flores@example.com',         'Peruana', '1980-01-14',
         'Prefiere habitación cercana al ascensor'),
    ( 7, 'DNI', '43905172', 'Jorge Luis',          'Chávez Navarro',
         '982346710', 'jorge.chavez@example.com',        'Peruana', '1986-12-08', NULL),
    ( 8, 'DNI', '72046381', 'Patricia Milagros',   'Herrera Campos',
         '936820415', 'patricia.herrera@example.com',    'Peruana', '1993-04-21', NULL),
    ( 9, 'DNI', '76318420', 'Diego Alonso',        'Zegarra Bustamante',
         '958403127', 'diego.zegarra@example.com',       'Peruana', '1995-08-17', NULL),
    (10, 'DNI', '47592038', 'Karla Vanessa',       'Aguilar Ramos',
         '947205839', 'karla.aguilar@example.com',       'Peruana', '1990-02-27',
         'Prefiere cama matrimonial'),
    (11, 'DNI', '08154729', 'Miguel Ángel',        'Cárdenas Silva',
         '965018342', 'miguel.cardenas@example.com',     'Peruana', '1969-06-05', NULL),
    (12, 'DNI', '44830917', 'Lucía Carolina',      'Paredes Vega',
         '972634810', 'lucia.paredes@example.com',       'Peruana', '1987-10-11', NULL),
    (13, 'DNI', '42175603', 'Fernando Javier',     'Salinas Ortiz',
         '984157029', 'fernando.salinas@example.com',    'Peruana', '1982-03-03', NULL),
    (14, 'DNI', '73904256', 'Gabriela Alejandra',  'Reyes Cabrera',
         '951730284', 'gabriela.reyes@example.com',      'Peruana', '1994-09-09', NULL),
    (15, 'DNI', '23918470', 'Walter',              'Huamán Quispe',
         '943086215', NULL,                              'Peruana', '1975-04-28', NULL),
    (16, 'DNI', '75120938', 'Sofía Isabel',        'Palomino Guerra',
         '990318472', 'sofia.palomino@example.com',      'Peruana', '1998-01-23', NULL),
    (17, 'DNI', '09463185', 'Héctor Manuel',       'Benavides Ruiz',
         '961479203', 'hector.benavides@example.com',    'Peruana', '1976-07-15', NULL),
    (18, 'DNI', '41298067', 'Claudia Beatriz',     'Arévalo Muñoz',
         '978205413', 'claudia.arevalo@example.com',     'Peruana', '1985-11-30',
         'Viaja en grupo; prefiere habitaciones contiguas'),
    (19, 'DNI', '40873519', 'Raúl Eduardo',        'Villanueva Sánchez',
         '952861047', 'raul.villanueva@example.com',     'Peruana', '1981-05-06', NULL),
    (20, 'DNI', '71835264', 'Natalia Cristina',    'Rivas Montoya',
         '939270581', 'natalia.rivas@example.com',       'Peruana', '1992-12-18', NULL),
    (21, 'DNI', '06382741', 'Julio César',         'Espinoza Vidal',
         '964512830', NULL,                              'Peruana', '1970-02-02', NULL),
    (22, 'DNI', '74650193', 'Valeria Stefany',     'León Carrillo',
         '987036152', 'valeria.leon@example.com',        'Peruana', '1996-06-14', NULL),
    (23, 'DNI', '45018237', 'Óscar Daniel',        'Lozano Peña',
         '941628305', 'oscar.lozano@example.com',        'Peruana', '1988-08-08', NULL),
    (24, 'DNI', '10726958', 'Milagros del Pilar',  'Cáceres Bravo',
         '955284037', NULL,                              'Peruana', '1983-10-26', NULL),
    -- Huéspedes extranjeros
    (25, 'pasaporte', 'AAB482913', 'Santiago Nicolás',   'Giménez Ferreyra',
         '+54 9 11 5821 4630', 'santiago.gimenez@example.com',  'Argentina',      '1985-03-20',
         'Prefiere habitación con vista a la calle'),
    (26, 'pasaporte', 'AAC107452', 'Camila Belén',       'Rodríguez Ledesma',
         '+54 9 351 604 8821', 'camila.rodriguez@example.com',  'Argentina',      '1990-09-12', NULL),
    (27, 'pasaporte', 'F2471893',  'Cristóbal Andrés',   'Soto Maldonado',
         '+56 9 7413 2085',    'cristobal.soto@example.com',    'Chilena',        '1987-04-04', NULL),
    (28, 'pasaporte', 'AX734108',  'Juliana Marcela',    'Restrepo Ocampo',
         '+57 311 482 7096',   'juliana.restrepo@example.com',  'Colombiana',     '1991-01-17',
         'Llega en vuelo nocturno; solicita ingreso tardío'),
    (29, 'pasaporte', 'G38472951', 'Alejandro Iván',     'Hernández Cervantes',
         '+52 55 4817 3209',   'alejandro.hernandez@example.com', 'Mexicana',      '1983-08-29', NULL),
    (30, 'pasaporte', 'XAB482761', 'Beatriz',            'Iglesias Fuentes',
         '+34 612 804 731',    'beatriz.iglesias@example.com',  'Española',       '1979-10-10', NULL),
    (31, 'pasaporte', '563018274', 'Daniel Robert',      'Whitfield',
         '+1 415 555 0172',    'daniel.whitfield@example.com',  'Estadounidense', '1980-07-07',
         'Prefiere habitación en piso alto'),
    (32, 'pasaporte', '618240935', 'Emily Rose',         'Hammond',
         '+1 415 555 0198',    'emily.hammond@example.com',     'Estadounidense', '1993-03-15', NULL),
    (33, 'pasaporte', 'FM482017',  'Gabriel Henrique',   'Oliveira Souza',
         '+55 11 91234 5780',  'gabriel.oliveira@example.com',  'Brasileña',      '1989-11-11',
         'Prefiere habitación silenciosa'),
    (34, 'pasaporte', 'C01X84629', 'Lukas Matthias',     'Schneider',
         '+49 151 2345 6789',  'lukas.schneider@example.com',   'Alemana',        '1986-02-24', NULL),
    (35, 'pasaporte', '19AB48273', 'Camille Marie',      'Duval',
         '+33 6 12 34 56 78',  'camille.duval@example.com',     'Francesa',       '1994-06-30', NULL),
    (36, 'CE',        '002418756', 'Paúl Santiago',      'Villacís Andrade',
         '958174026',          'paul.villacis@example.com',     'Ecuatoriana',    '1984-12-02', NULL),
    (37, 'pasaporte', 'XDB906143', 'Álvaro',             'Jiménez Sanz',
         '+34 655 219 084',    'alvaro.jimenez@example.com',    'Española',       '1977-02-12', NULL);


-- ---------------------------------------------------------------------
-- 5. RESERVAS (31)
--    Grupos:  1-12 y 19 finalizadas | 13-18 en estadía | 20-26 confirmadas
--             27-31 con cancelación (total o parcial)
--    Canales: 1 presencial, 2 telefono, 3 whatsapp
--    fecha_modificacion se informa solo donde hubo un cambio posterior.
-- ---------------------------------------------------------------------
INSERT INTO reserva
    (id_reserva, id_huesped_titular, id_canal, id_usuario_creador,
     id_usuario_modificador, fecha_creacion, fecha_modificacion)
OVERRIDING SYSTEM VALUE
VALUES
    -- Estadías finalizadas
    ( 1,  3, 2, 2, NULL, '2026-08-05 10:20:00-05', NULL),
    ( 2, 25, 3, 3, NULL, '2026-08-10 11:45:00-05', NULL),
    ( 3,  5, 1, 2, NULL, '2026-08-23 15:40:00-05', NULL),
    ( 4, 31, 2, 1, NULL, '2026-08-28 16:50:00-05', NULL),
    ( 5,  2, 3, 3, NULL, '2026-09-01 11:00:00-05', NULL),
    ( 6, 28, 3, 2, NULL, '2026-09-06 09:15:00-05', NULL),
    ( 7,  8, 2, 3, NULL, '2026-09-15 16:10:00-05', NULL),
    ( 8, 33, 3, 3, NULL, '2026-09-20 13:50:00-05', NULL),
    ( 9, 12, 1, 2, NULL, '2026-10-03 13:20:00-05', NULL),
    (10, 29, 2, 1, NULL, '2026-10-02 17:45:00-05', NULL),
    (11,  6, 3, 3, NULL, '2026-10-05 08:30:00-05', NULL),
    (12, 34, 3, 2, NULL, '2026-10-01 09:00:00-05', NULL),
    -- Estadías en curso al 18/10/2026
    (13,  1, 2, 3, 2,    '2026-10-08 12:50:00-05', '2026-10-14 10:20:00-05'),
    (14, 30, 3, 2, NULL, '2026-10-01 09:40:00-05', NULL),
    (15, 27, 3, 3, NULL, '2026-10-10 18:45:00-05', NULL),
    (16, 21, 1, 2, NULL, '2026-10-17 12:30:00-05', NULL),
    (17, 36, 2, 3, NULL, '2026-10-11 11:20:00-05', NULL),
    (18, 14, 3, 3, NULL, '2026-10-12 17:30:00-05', NULL),
    -- Check-out del día (18/10/2026)
    (19, 17, 1, 2, NULL, '2026-10-15 13:00:00-05', NULL),
    -- Reservas confirmadas (futuras)
    (20,  4, 3, 3, NULL, '2026-10-14 12:20:00-05', NULL),
    (21, 35, 3, 2, 2,    '2026-10-09 14:50:00-05', '2026-10-16 10:05:00-05'),
    (22, 10, 2, 3, NULL, '2026-10-16 15:50:00-05', NULL),
    (23, 32, 3, 3, NULL, '2026-10-17 20:00:00-05', NULL),
    (24,  9, 1, 2, NULL, '2026-10-18 10:50:00-05', NULL),
    (25, 11, 2, 1, NULL, '2026-10-02 09:40:00-05', NULL),
    (26, 18, 3, 3, NULL, '2026-10-15 18:20:00-05', NULL),
    -- Reservas con cancelación
    (27,  7, 2, 2, 2,    '2026-09-02 10:10:00-05', '2026-09-10 14:05:00-05'),
    (28, 26, 3, 3, 3,    '2026-09-25 15:30:00-05', '2026-10-03 16:25:00-05'),
    (29, 13, 3, 2, 2,    '2026-10-06 10:00:00-05', '2026-10-16 10:20:00-05'),
    (30, 22, 3, 3, 3,    '2026-10-12 08:50:00-05', '2026-10-17 18:25:00-05'),
    (31, 20, 2, 2, 2,    '2026-10-07 12:30:00-05', '2026-10-18 10:05:00-05');


-- ---------------------------------------------------------------------
-- 6. DETALLES DE RESERVA (34)
--    * Las cancelaciones se registran en la sección 8: allí un trigger pasa
--      el detalle a 'cancelada'. Por eso los detalles 29, 30, 32, 33 y 34
--      se insertan aquí como 'confirmada'.
--    * Los estados 'en_estadia' y 'finalizada' exigen las fechas reales de
--      check-in/check-out que pide la restricción ck_detalle_estado_fechas.
-- ---------------------------------------------------------------------
INSERT INTO detalle_reserva
    (id_detalle, id_reserva, id_habitacion,
     fecha_entrada_prevista, fecha_salida_prevista,
     checkin_real, checkout_real, tarifa_noche, estado)
OVERRIDING SYSTEM VALUE
VALUES
    -- Finalizadas
    ( 1,  1,  3, '2026-08-14', '2026-08-16', '2026-08-14 14:35:00-05', '2026-08-16 11:10:00-05', 130.00, 'finalizada'),
    ( 2,  2,  5, '2026-08-21', '2026-08-25', '2026-08-21 15:05:00-05', '2026-08-25 10:40:00-05', 150.00, 'finalizada'),
    ( 3,  3,  6, '2026-08-23', '2026-08-26', '2026-08-23 16:00:00-05', '2026-08-26 09:30:00-05',  80.00, 'finalizada'),
    ( 4,  4, 16, '2026-09-03', '2026-09-07', '2026-09-03 15:10:00-05', '2026-09-07 11:00:00-05', 300.00, 'finalizada'),
    ( 5,  5,  7, '2026-09-11', '2026-09-13', '2026-09-11 15:20:00-05', '2026-09-13 11:00:00-05', 130.00, 'finalizada'),
    ( 6,  5,  8, '2026-09-11', '2026-09-13', '2026-09-11 15:25:00-05', '2026-09-13 11:05:00-05', 130.00, 'finalizada'),
    ( 7,  6,  9, '2026-09-19', '2026-09-22', '2026-09-19 23:40:00-05', '2026-09-22 10:20:00-05', 150.00, 'finalizada'),
    ( 8,  7, 12, '2026-09-25', '2026-09-27', '2026-09-25 14:15:00-05', '2026-09-27 10:05:00-05', 140.00, 'finalizada'),
    -- Tarifa congelada: reservada a 320.00 y la suite 401 hoy cuesta 340.00
    ( 9,  8, 17, '2026-09-28', '2026-10-02', '2026-09-28 14:50:00-05', '2026-10-02 11:30:00-05', 320.00, 'finalizada'),
    (10,  9,  1, '2026-10-03', '2026-10-05', '2026-10-03 13:40:00-05', '2026-10-05 10:15:00-05',  90.00, 'finalizada'),
    (11, 10,  3, '2026-10-06', '2026-10-09', '2026-10-06 15:30:00-05', '2026-10-09 10:50:00-05', 140.00, 'finalizada'),
    (12, 11, 10, '2026-10-09', '2026-10-12', '2026-10-09 14:20:00-05', '2026-10-12 11:00:00-05', 160.00, 'finalizada'),
    (13, 12, 11, '2026-10-10', '2026-10-14', '2026-10-10 16:10:00-05', '2026-10-14 09:45:00-05', 200.00, 'finalizada'),
    -- En estadía (check-in realizado, sin check-out)
    (14, 13,  4, '2026-10-16', '2026-10-20', '2026-10-16 15:00:00-05', NULL, 140.00, 'en_estadia'),
    (15, 14, 16, '2026-10-15', '2026-10-21', '2026-10-15 16:20:00-05', NULL, 320.00, 'en_estadia'),
    (16, 15,  2, '2026-10-17', '2026-10-19', '2026-10-17 18:20:00-05', NULL,  90.00, 'en_estadia'),
    (17, 16,  6, '2026-10-17', '2026-10-22', '2026-10-17 14:00:00-05', NULL,  90.00, 'en_estadia'),
    (18, 17, 14, '2026-10-16', '2026-10-19', '2026-10-16 17:45:00-05', NULL, 210.00, 'en_estadia'),
    (19, 18,  7, '2026-10-18', '2026-10-20', '2026-10-18 12:30:00-05', NULL, 140.00, 'en_estadia'),
    -- Check-out del día
    (20, 19,  1, '2026-10-15', '2026-10-18', '2026-10-15 13:10:00-05', '2026-10-18 09:20:00-05',  90.00, 'finalizada'),
    -- Confirmadas
    (21, 20,  5, '2026-10-23', '2026-10-26', NULL, NULL, 160.00, 'confirmada'),
    (22, 21, 18, '2026-10-28', '2026-11-03', NULL, NULL, 360.00, 'confirmada'),
    (23, 22, 12, '2026-11-06', '2026-11-08', NULL, NULL, 150.00, 'confirmada'),
    (24, 22, 15, '2026-11-06', '2026-11-08', NULL, NULL, 210.00, 'confirmada'),
    (25, 23, 17, '2026-11-20', '2026-11-24', NULL, NULL, 340.00, 'confirmada'),
    (26, 24,  9, '2026-11-14', '2026-11-16', NULL, NULL, 160.00, 'confirmada'),
    (27, 25, 13, '2026-10-21', '2026-10-23', NULL, NULL, 170.00, 'confirmada'),
    (28, 26, 16, '2026-12-24', '2026-12-28', NULL, NULL, 320.00, 'confirmada'),
    -- Detalles que se cancelan en la sección 8 (se insertan 'confirmada')
    (29, 27, 15, '2026-09-18', '2026-09-20', NULL, NULL, 200.00, 'confirmada'),
    (30, 28, 16, '2026-10-05', '2026-10-08', NULL, NULL, 300.00, 'confirmada'),
    (31, 29,  3, '2026-10-30', '2026-11-02', NULL, NULL, 140.00, 'confirmada'),
    (32, 29,  4, '2026-10-30', '2026-11-02', NULL, NULL, 140.00, 'confirmada'),
    (33, 30,  8, '2026-11-12', '2026-11-14', NULL, NULL, 140.00, 'confirmada'),
    (34, 31, 10, '2026-12-18', '2026-12-21', NULL, NULL, 160.00, 'confirmada');


-- ---------------------------------------------------------------------
-- 7. HUÉSPEDES ALOJADOS POR DETALLE
--    Un trigger valida que no se supere la capacidad de la habitación.
-- ---------------------------------------------------------------------
INSERT INTO detalle_huesped (id_detalle, id_huesped) VALUES
    ( 1,  3), ( 1,  4),
    ( 2, 25), ( 2, 26),
    ( 3,  5),
    ( 4, 31), ( 4, 32),
    ( 5,  2), ( 5, 16),
    ( 6, 18), ( 6, 19),
    ( 7, 28),
    ( 8,  8), ( 8,  9),
    ( 9, 33),
    (10, 12),
    (11, 29),
    (12,  6), (12,  7),
    (13, 34),
    (14,  1), (14, 10),
    (15, 30), (15, 37),
    (16, 27),
    (17, 21),
    (18, 36), (18, 24), (18, 23),
    (19, 14),
    (20, 17),
    (21,  4), (21, 15),
    (22, 35),
    (23, 10), (23, 22),
    (24, 13), (24, 20),
    (25, 32),
    (26,  9), (26, 16),
    (27, 11),
    (28, 18), (28, 19), (28,  8),
    (29,  7),
    (30, 26),
    (31, 13), (31,  5),
    (32, 14), (32, 24),
    (33, 22),
    (34, 20);


-- ---------------------------------------------------------------------
-- 8. CANCELACIONES (una por detalle)
--    El trigger trg_cancelacion_insert cambia el estado del detalle a
--    'cancelada'. Penalidad 0 = cancelación sin costo.
-- ---------------------------------------------------------------------
INSERT INTO cancelacion
    (id_detalle, fecha_cancelacion, motivo, penalidad)
VALUES
    (29, '2026-09-10 14:00:00-05', 'Cambio de fecha de viaje por motivos laborales',  0.00),
    (30, '2026-10-03 16:20:00-05', 'Vuelo cancelado por la aerolínea',              300.00),
    (32, '2026-10-16 10:15:00-05', 'Un integrante del grupo no podrá viajar',        70.00),
    (33, '2026-10-17 18:20:00-05', 'Reprogramación por cambio de agenda',             0.00),
    (34, '2026-10-18 10:00:00-05', 'Cambio de planes familiares',                    80.00);


-- ---------------------------------------------------------------------
-- 9. PAGOS (anticipos, saldos y reembolsos)
--    Quedan con saldo pendiente: reservas 13 a 18, 20 a 26 y 29.
--    Quedan con reembolso pendiente: reserva 31.
-- ---------------------------------------------------------------------
INSERT INTO pago (id_reserva, tipo, metodo, fecha_pago, monto) VALUES
    -- Reserva 1
    ( 1, 'anticipo',  'efectivo',      '2026-08-05 10:30:00-05',  130.00),
    ( 1, 'saldo',     'efectivo',      '2026-08-16 10:55:00-05',  130.00),
    -- Reserva 2
    ( 2, 'anticipo',  'transferencia', '2026-08-10 12:00:00-05',  200.00),
    ( 2, 'saldo',     'tarjeta',       '2026-08-25 10:30:00-05',  400.00),
    -- Reserva 3
    ( 3, 'anticipo',  'efectivo',      '2026-08-23 15:50:00-05',   80.00),
    ( 3, 'saldo',     'efectivo',      '2026-08-26 09:20:00-05',  160.00),
    -- Reserva 4
    ( 4, 'anticipo',  'tarjeta',       '2026-08-28 17:00:00-05',  400.00),
    ( 4, 'saldo',     'tarjeta',       '2026-09-07 11:00:00-05',  800.00),
    -- Reserva 5
    ( 5, 'anticipo',  'transferencia', '2026-09-01 11:15:00-05',  200.00),
    ( 5, 'saldo',     'tarjeta',       '2026-09-13 10:45:00-05',  320.00),
    -- Reserva 6
    ( 6, 'anticipo',  'transferencia', '2026-09-06 09:30:00-05',  150.00),
    ( 6, 'saldo',     'tarjeta',       '2026-09-22 10:30:00-05',  300.00),
    -- Reserva 7
    ( 7, 'anticipo',  'efectivo',      '2026-09-15 16:20:00-05',  100.00),
    ( 7, 'saldo',     'efectivo',      '2026-09-27 10:10:00-05',  180.00),
    -- Reserva 8
    ( 8, 'anticipo',  'tarjeta',       '2026-09-20 14:00:00-05',  500.00),
    ( 8, 'saldo',     'tarjeta',       '2026-10-02 11:20:00-05',  780.00),
    -- Reserva 9
    ( 9, 'anticipo',  'efectivo',      '2026-10-03 13:30:00-05',   90.00),
    ( 9, 'saldo',     'efectivo',      '2026-10-05 10:05:00-05',   90.00),
    -- Reserva 10
    (10, 'anticipo',  'tarjeta',       '2026-10-02 18:00:00-05',  140.00),
    (10, 'saldo',     'tarjeta',       '2026-10-09 10:40:00-05',  280.00),
    -- Reserva 11
    (11, 'anticipo',  'transferencia', '2026-10-05 08:45:00-05',  160.00),
    (11, 'saldo',     'efectivo',      '2026-10-12 10:50:00-05',  320.00),
    -- Reserva 12
    (12, 'anticipo',  'tarjeta',       '2026-10-01 09:15:00-05',  300.00),
    (12, 'saldo',     'tarjeta',       '2026-10-14 09:40:00-05',  500.00),
    -- Reserva 13 (en estadía, saldo pendiente)
    (13, 'anticipo',  'transferencia', '2026-10-08 13:00:00-05',  200.00),
    -- Reserva 14 (en estadía, pago parcial)
    (14, 'anticipo',  'tarjeta',       '2026-10-01 10:00:00-05',  800.00),
    (14, 'saldo',     'tarjeta',       '2026-10-15 16:30:00-05',  600.00),
    -- Reservas 15 a 18 (en estadía, solo anticipo)
    (15, 'anticipo',  'transferencia', '2026-10-10 19:00:00-05',   90.00),
    (16, 'anticipo',  'efectivo',      '2026-10-17 14:10:00-05',  150.00),
    (17, 'anticipo',  'transferencia', '2026-10-11 11:30:00-05',  210.00),
    (18, 'anticipo',  'efectivo',      '2026-10-12 17:40:00-05',  140.00),
    -- Reserva 19 (check-out de hoy, pagada)
    (19, 'anticipo',  'efectivo',      '2026-10-15 13:20:00-05',   90.00),
    (19, 'saldo',     'efectivo',      '2026-10-18 09:15:00-05',  180.00),
    -- Reservas confirmadas
    (20, 'anticipo',  'transferencia', '2026-10-14 12:30:00-05',  160.00),
    (21, 'anticipo',  'tarjeta',       '2026-10-09 15:00:00-05',  700.00),
    (21, 'anticipo',  'tarjeta',       '2026-10-16 10:00:00-05',  500.00),
    (22, 'anticipo',  'efectivo',      '2026-10-16 16:00:00-05',  300.00),
    (23, 'anticipo',  'tarjeta',       '2026-10-17 20:10:00-05',  400.00),
    (24, 'anticipo',  'efectivo',      '2026-10-18 11:00:00-05',  160.00),
    (25, 'anticipo',  'transferencia', '2026-10-02 09:50:00-05',  170.00),
    (26, 'anticipo',  'transferencia', '2026-10-15 18:30:00-05',  640.00),
    -- Reserva 27: cancelada sin penalidad, anticipo devuelto
    (27, 'anticipo',  'transferencia', '2026-09-02 10:20:00-05',  200.00),
    (27, 'reembolso', 'transferencia', '2026-09-12 12:00:00-05',  200.00),
    -- Reserva 28: cancelada con penalidad de una noche; se devuelve el resto
    (28, 'anticipo',  'tarjeta',       '2026-09-25 15:40:00-05',  450.00),
    (28, 'reembolso', 'tarjeta',       '2026-10-05 11:30:00-05',  150.00),
    -- Reserva 29: cancelación parcial (una de las dos habitaciones)
    (29, 'anticipo',  'transferencia', '2026-10-06 10:10:00-05',  280.00),
    -- Reserva 30: cancelada sin penalidad, anticipo devuelto
    (30, 'anticipo',  'transferencia', '2026-10-12 09:00:00-05',  140.00),
    (30, 'reembolso', 'transferencia', '2026-10-17 18:30:00-05',  140.00),
    -- Reserva 31: cancelada con penalidad; reembolso aún no registrado
    (31, 'anticipo',  'transferencia', '2026-10-07 12:45:00-05',  320.00);


-- ---------------------------------------------------------------------
-- 10. AJUSTE DE SECUENCIAS
--     Las filas se insertaron con id explícito; las secuencias se
--     posicionan en el máximo para que los próximos INSERT sigan sin chocar.
-- ---------------------------------------------------------------------
SELECT setval(pg_get_serial_sequence('usuario',            'id_usuario'),    (SELECT max(id_usuario)    FROM usuario));
SELECT setval(pg_get_serial_sequence('habitacion',         'id_habitacion'), (SELECT max(id_habitacion) FROM habitacion));
SELECT setval(pg_get_serial_sequence('bloqueo_habitacion', 'id_bloqueo'),    (SELECT max(id_bloqueo)    FROM bloqueo_habitacion));
SELECT setval(pg_get_serial_sequence('huesped',            'id_huesped'),    (SELECT max(id_huesped)    FROM huesped));
SELECT setval(pg_get_serial_sequence('reserva',            'id_reserva'),    (SELECT max(id_reserva)    FROM reserva));
SELECT setval(pg_get_serial_sequence('detalle_reserva',    'id_detalle'),    (SELECT max(id_detalle)    FROM detalle_reserva));

COMMIT;


-- ---------------------------------------------------------------------
-- 11. CONSULTAS DE VERIFICACIÓN (opcionales; descomente para ejecutarlas)
-- ---------------------------------------------------------------------
-- Filas por tabla
-- SELECT 'usuario' AS tabla, count(*) FROM usuario
-- UNION ALL SELECT 'habitacion',         count(*) FROM habitacion
-- UNION ALL SELECT 'bloqueo_habitacion', count(*) FROM bloqueo_habitacion
-- UNION ALL SELECT 'huesped',            count(*) FROM huesped
-- UNION ALL SELECT 'reserva',            count(*) FROM reserva
-- UNION ALL SELECT 'detalle_reserva',    count(*) FROM detalle_reserva
-- UNION ALL SELECT 'detalle_huesped',    count(*) FROM detalle_huesped
-- UNION ALL SELECT 'cancelacion',        count(*) FROM cancelacion
-- UNION ALL SELECT 'pago',               count(*) FROM pago;
--
-- Detalles por estado
-- SELECT estado, count(*) FROM detalle_reserva GROUP BY estado ORDER BY estado;
--
-- Saldos por reserva
-- SELECT * FROM v_saldo_reserva ORDER BY id_reserva;
--
-- Noches ocupadas por mes
-- SELECT * FROM v_noches_ocupadas_por_mes;
