package co.empresa.gestioncontratos.service;

import co.empresa.gestioncontratos.dto.TarifaDTO;
import co.empresa.gestioncontratos.entity.PlanTarifa;
import co.empresa.gestioncontratos.entity.Servicio;
import co.empresa.gestioncontratos.entity.Tarifa;
import co.empresa.gestioncontratos.enums.TipoPredio;
import co.empresa.gestioncontratos.repository.PlanTarifaRepository;
import co.empresa.gestioncontratos.repository.ServicioRepository;
import co.empresa.gestioncontratos.repository.TarifaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TarifaService {

    private final TarifaRepository tarifaRepository;
    private final PlanTarifaRepository planTarifaRepository;
    private final ServicioRepository servicioRepository;

    // ==================== CONSULTAS ====================

    @Transactional(readOnly = true)
    public List<Tarifa> listarTodas() {
        log.info("Listando todas las tarifas");
        return tarifaRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Page<Tarifa> listarTodasPaginado(Pageable pageable) {
        return tarifaRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public List<Tarifa> listarPorPlanTarifa(UUID planTarifaUuid) {
        log.info("Listando tarifas del plan: {}", planTarifaUuid);
        
        PlanTarifa planTarifa = planTarifaRepository.findByUuid(planTarifaUuid)
            .orElseThrow(() -> new RuntimeException("Plan de tarifa no encontrado"));
            
        return planTarifa.getTarifas().stream()
            .filter(Tarifa::getActivo)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PlanTarifa>  listarTodos() {
        log.info("Listando tarifas del plan: {}", "");
        
        List<PlanTarifa> planTarifa = planTarifaRepository.findAll();
            
            
        return planTarifa;
    }

    @Transactional(readOnly = true)
    public List<Tarifa> listarPorServicio(UUID servicioUuid) {
        log.info("Listando tarifas del servicio: {}", servicioUuid);
        
        Servicio servicio = servicioRepository.findByUuid(servicioUuid)
            .orElseThrow(() -> new RuntimeException("Servicio no encontrado"));
            
        return tarifaRepository.findByServicioOrderByPrecioUrbanoAsc(servicio);
    }

    @Transactional(readOnly = true)
    public Tarifa buscarPorUuid(UUID uuid) {
        return tarifaRepository.findByUuid(uuid)
            .orElseThrow(() -> new RuntimeException("Tarifa no encontrada: " + uuid));
    }

    @Transactional(readOnly = true)
    public Optional<Tarifa> buscarPorPlanYServicio(UUID planTarifaUuid, UUID servicioUuid) {
        log.info("Buscando tarifa para plan {} y servicio {}", planTarifaUuid, servicioUuid);
        
        return tarifaRepository.findByPlanTarifaUuidAndServicioUuid(planTarifaUuid, servicioUuid);
    }

    // ==================== GESTIÓN DE TARIFAS ====================

    public Tarifa crear(TarifaDTO tarifaDTO) {
        log.info("Creando nueva tarifa para servicio: {}", tarifaDTO.getServicioUuid());
        
        // Validar que no exista ya una tarifa para el mismo plan y servicio
        Optional<Tarifa> tarifaExistente = buscarPorPlanYServicio(
            tarifaDTO.getPlanTarifaUuid(), 
            tarifaDTO.getServicioUuid()
        );
        
        if (tarifaExistente.isPresent()) {
            throw new RuntimeException("Ya existe una tarifa para este servicio en el plan seleccionado");
        }
        
        // Validar que los precios sean positivos
        if (tarifaDTO.getPrecioUrbano().compareTo(BigDecimal.ZERO) <= 0 ||
            tarifaDTO.getPrecioRural().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Los precios deben ser mayores a cero");
        }
        
        PlanTarifa planTarifa = planTarifaRepository.findByUuid(tarifaDTO.getPlanTarifaUuid())
            .orElseThrow(() -> new RuntimeException("Plan de tarifa no encontrado"));
            
        Servicio servicio = servicioRepository.findByUuid(tarifaDTO.getServicioUuid())
            .orElseThrow(() -> new RuntimeException("Servicio no encontrado"));
        
        Tarifa tarifa = Tarifa.builder()
            .planTarifa(planTarifa)
            .servicio(servicio)
            .precioUrbano(tarifaDTO.getPrecioUrbano())
            .precioRural(tarifaDTO.getPrecioRural())
            .activo(true)
            .build();
        
        return tarifaRepository.save(tarifa);
    }

    public Tarifa actualizar(UUID uuid, TarifaDTO tarifaDTO) {
        log.info("Actualizando tarifa: {}", uuid);
        
        Tarifa tarifa = buscarPorUuid(uuid);
        
        // Validar que los precios sean positivos
        if (tarifaDTO.getPrecioUrbano().compareTo(BigDecimal.ZERO) <= 0 ||
            tarifaDTO.getPrecioRural().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Los precios deben ser mayores a cero");
        }

        // Si cambia el plan o servicio, validar que no exista duplicado
        if (!tarifa.getPlanTarifa().getUuid().equals(tarifaDTO.getPlanTarifaUuid()) ||
            !tarifa.getServicio().getUuid().equals(tarifaDTO.getServicioUuid())) {
            
            Optional<Tarifa> tarifaExistente = buscarPorPlanYServicio(
                tarifaDTO.getPlanTarifaUuid(), 
                tarifaDTO.getServicioUuid()
            );
            
            if (tarifaExistente.isPresent() && !tarifaExistente.get().getUuid().equals(uuid)) {
                throw new RuntimeException("Ya existe una tarifa para este servicio en el plan seleccionado");
            }
        }
        
        if (!tarifa.getPlanTarifa().getUuid().equals(tarifaDTO.getPlanTarifaUuid())) {
            PlanTarifa planTarifa = planTarifaRepository.findByUuid(tarifaDTO.getPlanTarifaUuid())
                .orElseThrow(() -> new RuntimeException("Plan de tarifa no encontrado"));
            tarifa.setPlanTarifa(planTarifa);
        }
        
        if (!tarifa.getServicio().getUuid().equals(tarifaDTO.getServicioUuid())) {
            Servicio servicio = servicioRepository.findByUuid(tarifaDTO.getServicioUuid())
                .orElseThrow(() -> new RuntimeException("Servicio no encontrado"));
            tarifa.setServicio(servicio);
        }
        
        tarifa.setPrecioUrbano(tarifaDTO.getPrecioUrbano());
        tarifa.setPrecioRural(tarifaDTO.getPrecioRural());
        
        return tarifaRepository.save(tarifa);
    }

    public void cambiarEstado(UUID uuid) {
        log.info("Cambiando estado de la tarifa: {}", uuid);
        
        Tarifa tarifa = buscarPorUuid(uuid);
        tarifa.setActivo(!tarifa.getActivo());
        
        tarifaRepository.save(tarifa);
    }

    public void eliminar(UUID uuid) {
        log.info("Eliminando tarifa: {}", uuid);
        
        Tarifa tarifa = buscarPorUuid(uuid);
        
        // Aquí podrías agregar validaciones adicionales
        // Por ejemplo, verificar que no haya actividades usando esta tarifa
        
        tarifaRepository.delete(tarifa);
    }

    // ==================== CONSULTAS ESPECÍFICAS ====================

    @Transactional(readOnly = true)
    public BigDecimal obtenerPrecioPorServicioYTipo(UUID planTarifaUuid, UUID servicioUuid, TipoPredio tipoPredio) {
        log.info("Obteniendo precio para servicio {} tipo {} en plan {}", 
            servicioUuid, tipoPredio, planTarifaUuid);
        
        Optional<Tarifa> tarifa = buscarPorPlanYServicio(planTarifaUuid, servicioUuid);
        
        if (tarifa.isEmpty()) {
            throw new RuntimeException("No se encontró tarifa para el servicio en el plan especificado");
        }
        
        return tarifa.get().getPrecioPorTipo(tipoPredio.name());
    }

    @Transactional(readOnly = true)
    public List<TarifaDTO> listarTarifasConDetalles(UUID planTarifaUuid) {
        log.info("Listando tarifas con detalles del plan: {}", planTarifaUuid);
        
        List<Tarifa> tarifas = listarPorPlanTarifa(planTarifaUuid);
        
        return tarifas.stream()
            .map(this::convertirADTOConDetalles)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> obtenerResumenTarifasPorPlan(UUID planTarifaUuid) {
        log.info("Obteniendo resumen de tarifas del plan: {}", planTarifaUuid);
        
        List<Tarifa> tarifas = listarPorPlanTarifa(planTarifaUuid);
        
        Map<String, Object> resumen = new HashMap<>();
        resumen.put("totalTarifas", tarifas.size());
        resumen.put("tarifasActivas", tarifas.stream().filter(Tarifa::getActivo).count());
        
        if (!tarifas.isEmpty()) {
            double promedioUrbano = tarifas.stream()
                .mapToDouble(t -> t.getPrecioUrbano().doubleValue())
                .average()
                .orElse(0.0);

            double promedioRural = tarifas.stream()
                .mapToDouble(t -> t.getPrecioRural().doubleValue())
                .average()
                .orElse(0.0);
                
            resumen.put("precioPromedioUrbano", promedioUrbano);
            resumen.put("precioPromedioRural", promedioRural);
        }
        
        return resumen;
    }

    // ==================== OPERACIONES MASIVAS ====================

    public List<Tarifa> crearTarifasMasivo(UUID planTarifaUuid, List<TarifaDTO> tarifasDTO) {
        log.info("Creando {} tarifas para el plan {}", tarifasDTO.size(), planTarifaUuid);
        
        List<Tarifa> tarifasCreadas = new ArrayList<>();
        List<String> errores = new ArrayList<>();
        
        for (int i = 0; i < tarifasDTO.size(); i++) {
            TarifaDTO dto = tarifasDTO.get(i);
            dto.setPlanTarifaUuid(planTarifaUuid);
            
            try {
                Tarifa tarifa = crear(dto);
                tarifasCreadas.add(tarifa);
            } catch (Exception e) {
                errores.add(String.format("Fila %d: %s", i + 1, e.getMessage()));
            }
        }
        
        if (!errores.isEmpty()) {
            log.warn("Errores durante la creación masiva: {}", errores);
            throw new RuntimeException("Se crearon " + tarifasCreadas.size() + 
                " tarifas con errores: " + String.join(", ", errores));
        }
        
        return tarifasCreadas;
    }

    public void actualizarPreciosMasivo(UUID planTarifaUuid, BigDecimal porcentajeAumento) {
        log.info("Actualizando precios del plan {} con aumento del {}%", planTarifaUuid, porcentajeAumento);
        
        if (porcentajeAumento.compareTo(new BigDecimal("-100")) < 0 ||
            porcentajeAumento.compareTo(new BigDecimal("1000")) > 0) {
            throw new RuntimeException("El porcentaje de aumento debe estar entre -100% y 1000%");
        }
        
        List<Tarifa> tarifas = listarPorPlanTarifa(planTarifaUuid);
        BigDecimal factor = BigDecimal.ONE.add(porcentajeAumento.divide(new BigDecimal("100")));
        
        for (Tarifa tarifa : tarifas) {
            tarifa.setPrecioUrbano(tarifa.getPrecioUrbano());
            tarifa.setPrecioRural(tarifa.getPrecioRural());
            tarifaRepository.save(tarifa);
        }
        
        log.info("Actualizados {} precios", tarifas.size());
    }

    @Transactional(readOnly = true)
    public Page<Tarifa> buscarConFiltros(String filtro, Boolean activo, UUID planTarifaUuid, 
                                        UUID servicioUuid, Pageable pageable) {
        log.info("Buscando tarifas con filtros - texto: {}, activo: {}, plan: {}, servicio: {}", 
            filtro, activo, planTarifaUuid, servicioUuid);
        
        Specification<Tarifa> spec = Specification.where(null);
        
        // Filtro por texto (busca en nombre del plan o servicio)
        if (filtro != null && !filtro.trim().isEmpty()) {
            spec = spec.and((root, query, criteriaBuilder) -> {
                String filtroLike = "%" + filtro.toLowerCase() + "%";
                return criteriaBuilder.or(
                    criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("planTarifa").get("nombre")), 
                        filtroLike
                    ),
                    criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("servicio").get("nombre")), 
                        filtroLike
                    )
                );
            });
        }
        
        // Filtro por estado activo
        if (activo != null) {
            spec = spec.and((root, query, criteriaBuilder) -> 
                criteriaBuilder.equal(root.get("activo"), activo)
            );
        }
        
        // Filtro por plan de tarifa
        if (planTarifaUuid != null) {
            spec = spec.and((root, query, criteriaBuilder) -> 
                criteriaBuilder.equal(root.get("planTarifa").get("uuid"), planTarifaUuid)
            );
        }
        
        // Filtro por servicio
        if (servicioUuid != null) {
            spec = spec.and((root, query, criteriaBuilder) -> 
                criteriaBuilder.equal(root.get("servicio").get("uuid"), servicioUuid)
            );
        }
        
        return tarifaRepository.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> obtenerResumenGeneral() {
        log.info("Obteniendo resumen general de tarifas");
        
        Map<String, Object> resumen = new HashMap<>();
        
        // Contar totales
        long totalTarifas = tarifaRepository.count();
        long tarifasActivas = tarifaRepository.countByActivo(true);
        long tarifasInactivas = totalTarifas - tarifasActivas;
        
        resumen.put("totalTarifas", totalTarifas);
        resumen.put("tarifasActivas", tarifasActivas);
        resumen.put("tarifasInactivas", tarifasInactivas);
        
        // Estadísticas por plan de tarifa
        List<Object[]> tarifasPorPlan = tarifaRepository.contarTarifasPorPlan();
        Map<String, Long> distribucionPorPlan = tarifasPorPlan.stream()
            .collect(Collectors.toMap(
                obj -> (String) obj[0], // nombre del plan
                obj -> (Long) obj[1]    // cantidad
            ));
        resumen.put("tarifasPorPlan", distribucionPorPlan);
        
        // Promedios de precios
        if (totalTarifas > 0) {
            BigDecimal promedioUrbano = tarifaRepository.obtenerPromedioPrecioUrbano();
            BigDecimal promedioRural = tarifaRepository.obtenerPromedioPrecioRural();
            BigDecimal precioMinimoUrbano = tarifaRepository.obtenerPrecioMinimoUrbano();
            BigDecimal precioMaximoUrbano = tarifaRepository.obtenerPrecioMaximoUrbano();
            
            resumen.put("promedioUrbano", promedioUrbano);
            resumen.put("promedioRural", promedioRural);
            resumen.put("precioMinimoUrbano", precioMinimoUrbano);
            resumen.put("precioMaximoUrbano", precioMaximoUrbano);
        }
        
        return resumen;
    }
    // ==================== UTILIDADES ====================

    public TarifaDTO convertirADTO(Tarifa tarifa) {
        return TarifaDTO.builder()
            .uuid(tarifa.getUuid())
            .planTarifaUuid(tarifa.getPlanTarifa().getUuid())
            .servicioUuid(tarifa.getServicio().getUuid())
            .precioUrbano(tarifa.getPrecioUrbano())
            .precioRural(tarifa.getPrecioRural())
            .activo(tarifa.getActivo())
            .build();
    }

    public TarifaDTO convertirADTOConDetalles(Tarifa tarifa) {
        TarifaDTO dto = convertirADTO(tarifa);
        dto.setPlanTarifaNombre(tarifa.getPlanTarifa().getNombre());
        dto.setServicioNombre(tarifa.getServicio().getNombre());
        return dto;
    }

    public Map<String, Object> cargarTarifasDesdeExcel(MultipartFile archivo, UUID planTarifaUuid) {
        log.info("Procesando archivo Excel de tarifas para plan: {}", planTarifaUuid);
        
        Map<String, Object> resultado = new HashMap<>();
        List<String> errores = new ArrayList<>();
        List<TarifaDTO> tarifasCreadas = new ArrayList<>();
        int filasProcesadas = 0;
        int tarifasExitosas = 0;
        
        try {
            PlanTarifa planTarifa = planTarifaRepository.findByUuid(planTarifaUuid)
                .orElseThrow(() -> new RuntimeException("Plan de tarifa no encontrado"));
            
            Workbook workbook = WorkbookFactory.create(archivo.getInputStream());
            Sheet sheet = workbook.getSheetAt(0);
            
            // Validar encabezados (fila 0)
            Row headerRow = sheet.getRow(0);
            if (headerRow == null || !validarEncabezados(headerRow)) {
                throw new RuntimeException("El archivo no tiene el formato correcto. Descargue la plantilla.");
            }
            
            // Procesar filas de datos (desde fila 1)
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || estaFilaVacia(row)) continue;
                
                filasProcesadas++;
                
                try {
                    TarifaDTO tarifaDTO = procesarFilaExcel(row, planTarifaUuid);
                    Tarifa tarifa = crear(tarifaDTO);
                    tarifasCreadas.add(convertirADTO(tarifa));
                    tarifasExitosas++;
                    
                } catch (Exception e) {
                    errores.add("Fila " + (i + 1) + ": " + e.getMessage());
                }
            }
            
            workbook.close();
            
            resultado.put("filasProcesadas", filasProcesadas);
            resultado.put("tarifasCreadas", tarifasExitosas);
            resultado.put("errores", errores);
            resultado.put("tarifas", tarifasCreadas);
            
            log.info("Procesamiento completado: {} tarifas creadas de {} filas procesadas", 
                    tarifasExitosas, filasProcesadas);
            
            return resultado;
            
        } catch (Exception e) {
            log.error("Error procesando archivo Excel: ", e);
            throw new RuntimeException("Error al procesar archivo: " + e.getMessage());
        }
    }

    private boolean validarEncabezados(Row headerRow) {
        String[] encabezadosEsperados = {"CODIGO_SERVICIO", "PRECIO_URBANO", "PRECIO_RURAL"};
        
        for (int i = 0; i < encabezadosEsperados.length; i++) {
            Cell cell = headerRow.getCell(i);
            if (cell == null || !encabezadosEsperados[i].equals(cell.getStringCellValue().trim().toUpperCase())) {
                return false;
            }
        }
        return true;
    }

    private boolean estaFilaVacia(Row row) {
        for (int i = 0; i < 3; i++) {
            Cell cell = row.getCell(i);
            if (cell != null && !cell.getStringCellValue().trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private TarifaDTO procesarFilaExcel(Row row, UUID planTarifaUuid) {
        try {
            String codigoServicio = getCellValueAsString(row.getCell(0));
            BigDecimal precioUrbano = getCellValueAsBigDecimal(row.getCell(1));
            BigDecimal precioRural = getCellValueAsBigDecimal(row.getCell(2));
            
            if (codigoServicio == null || codigoServicio.trim().isEmpty()) {
                throw new RuntimeException("Código de servicio es requerido");
            }
            
            if (precioUrbano == null || precioUrbano.compareTo(BigDecimal.ZERO) <= 0) {
                throw new RuntimeException("Precio urbano debe ser mayor a 0");
            }
            
            if (precioRural == null || precioRural.compareTo(BigDecimal.ZERO) <= 0) {
                throw new RuntimeException("Precio rural debe ser mayor a 0");
            }
            Optional<Servicio> servicio = servicioRepository.findByNombre(codigoServicio);            
            UUID uuidServicio = servicio.get().getUuid();
            
            return TarifaDTO.builder()  
                .servicioUuid(uuidServicio)              
                .precioUrbano(precioUrbano)
                .precioRural(precioRural)
                .planTarifaUuid(planTarifaUuid)
                .activo(true)
                .build();
                
        } catch (Exception e) {
            throw new RuntimeException("Error en formato de datos: " + e.getMessage());
        }
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return null;
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                return String.valueOf((long) cell.getNumericCellValue());
            default:
                return null;
        }
    }

    private BigDecimal getCellValueAsBigDecimal(Cell cell) {
        if (cell == null) return null;
        
        switch (cell.getCellType()) {
            case NUMERIC:
                return BigDecimal.valueOf(cell.getNumericCellValue());
            case STRING:
                try {
                    return new BigDecimal(cell.getStringCellValue());
                } catch (NumberFormatException e) {
                    throw new RuntimeException("Valor numérico inválido: " + cell.getStringCellValue());
                }
            default:
                return null;
        }
    }
    public byte[] generarPlantillaExcel() {
        try {
            // Usar HSSFWorkbook (formato .xls) en lugar de XSSFWorkbook
            HSSFWorkbook workbook = new HSSFWorkbook();
            HSSFSheet sheet = workbook.createSheet("Tarifas");
            
            // Crear encabezados
            HSSFRow headerRow = sheet.createRow(0);
            String[] headers = {"CODIGO_SERVICIO", "PRECIO_URBANO", "PRECIO_RURAL"};
            
            for (int i = 0; i < headers.length; i++) {
                HSSFCell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                
                // Estilo para encabezados
                HSSFCellStyle headerStyle = workbook.createCellStyle();
                HSSFFont font = workbook.createFont();
                font.setBold(true);
                headerStyle.setFont(font);
                cell.setCellStyle(headerStyle);
            }
            
            // Crear filas de ejemplo
            HSSFRow row1 = sheet.createRow(1);
            row1.createCell(0).setCellValue("REVISION CON CAMBIO DE MEDIDOR MONOFASICO BIFASICO O POLIFASICO DE MEDIDA DIRECTA. A");
            row1.createCell(1).setCellValue(50000.0);
            row1.createCell(2).setCellValue(45000.0);
            
            HSSFRow row2 = sheet.createRow(2);
            row2.createCell(0).setCellValue("REVISION CON CAMBIO DE MEDIDOR MONOFASICO BIFASICO O POLIFASICO DE MEDIDA DIRECTA. B");
            row2.createCell(1).setCellValue(75000.0);
            row2.createCell(2).setCellValue(70000.0);
            
            // Ajustar ancho de columnas
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            
            // Convertir a bytes
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            byte[] result = outputStream.toByteArray();
            workbook.close();
            
            return result;
            
        } catch (Exception e) {
            log.error("Error generando plantilla Excel: ", e);
            throw new RuntimeException("Error al generar plantilla: " + e.getMessage());
        }
    }    
    @Transactional(readOnly = true)
    public List<TarifaDTO> listarTarifasConFiltros(String filtro, Boolean activo, 
                                                UUID planTarifaUuid, UUID servicioUuid) {
        log.info("Listando tarifas con filtros - plan: {}, servicio: {}, activo: {}, filtro: {}", 
                planTarifaUuid, servicioUuid, activo, filtro);
        
        try {
            List<Tarifa> tarifas;
            
            if (planTarifaUuid != null || servicioUuid != null || activo != null || 
                (filtro != null && !filtro.trim().isEmpty())) {
                // Usar filtros específicos
                tarifas = tarifaRepository.buscarConFiltros(filtro, activo, planTarifaUuid, servicioUuid);
            } else {
                // Listar todas si no hay filtros
                tarifas = tarifaRepository.findAll();
            }
            
            return tarifas.stream()
                .map(this::convertirADTOConDetalles)
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            log.error("Error al listar tarifas con filtros: ", e);
            throw new RuntimeException("Error al obtener tarifas", e);
        }
    }    
}