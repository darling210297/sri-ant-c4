/*
 * Modelo C4 en Structurizr DSL (referencia / respaldo del diagrama de IcePanel).
 * Se puede visualizar en https://structurizr.com/dsl
 */
workspace "Sistema de Consulta SRI-ANT" "Tarea Arquitectura C4 - ISWZ2202" {

    model {
        usuario = person "Usuario" "Persona natural que consulta su información tributaria, vehicular y de licencia."

        sriCatastro = softwareSystem "SRI - Catastro de Contribuyentes" "API REST pública del SRI." "Externo"
        sriVehiculos = softwareSystem "SRI - Matriculación Vehicular" "API REST pública del SRI." "Externo"
        ant = softwareSystem "ANT - Portal de Citaciones" "Portal web de la ANT. Baja disponibilidad." "Externo"

        sistema = softwareSystem "Sistema de Consulta SRI-ANT" "Verifica contribuyente persona natural, vehículo y puntos de licencia." {
            web = container "Aplicación Web" "Formulario y visualización de resultados y avisos de datos no recientes." "React 18, Vite"
            api = container "API de Consultas" "API REST /api/v1, validación, integraciones y Cache-Aside + stale-if-error." "Java 17, Spring Boot 3" {
                controller = component "ConsultaController" "Contrato REST y validación." "Spring MVC"
                contribSvc = component "ContribuyenteService" "Verifica contribuyente y persona natural." "Spring Service"
                vehSvc = component "VehiculoService" "Información del vehículo." "Spring Service"
                licSvc = component "LicenciaService" "Puntos de licencia." "Spring Service"
                resilientCache = component "ResilientCache" "Cache-Aside + último dato bueno." "Java"
                sriClient = component "SriClient" "Adaptador SRI." "RestClient"
                antClient = component "AntClient" "Adaptador ANT." "RestClient + Jsoup"
            }
            cache = container "Caché distribuida" "Respuestas adecuadas con fecha de obtención y TTL." "Redis en la nube (Upstash)" "Database"
        }

        usuario -> web "Ingresa correo, RUC y placa" "HTTPS"
        web -> api "Consulta contribuyente, vehículo y puntos" "REST/JSON"
        api -> cache "Lee / escribe" "RESP sobre TLS"
        api -> sriCatastro "Verifica contribuyente y persona natural" "HTTPS REST"
        api -> sriVehiculos "Consulta vehículo por placa" "HTTPS REST"
        api -> ant "Consulta puntos de licencia" "HTTPS"

        web -> controller "Llama" "REST/JSON"
        controller -> contribSvc "Usa"
        controller -> vehSvc "Usa"
        controller -> licSvc "Usa"
        contribSvc -> resilientCache "Usa"
        vehSvc -> resilientCache "Usa"
        licSvc -> resilientCache "Usa"
        resilientCache -> cache "Lee / escribe"
        contribSvc -> sriClient "Usa"
        vehSvc -> sriClient "Usa"
        licSvc -> antClient "Usa"
        sriClient -> sriCatastro "GET existePorNumeroRuc / obtenerPorNumerosRuc"
        sriClient -> sriVehiculos "GET obtenerPorNumeroPlaca..."
        antClient -> ant "GET clp_grid_citaciones.jsp"
    }

    views {
        systemContext sistema "Contexto" { include * autolayout lr }
        container sistema "Contenedores" { include * autolayout lr }
        component api "Componentes" { include * autolayout lr }
        styles {
            element "Person" { shape Person background #08427b color #ffffff }
            element "Software System" { background #1168bd color #ffffff }
            element "Externo" { background #999999 color #ffffff }
            element "Container" { background #438dd5 color #ffffff }
            element "Database" { shape Cylinder }
            element "Component" { background #85bbf0 color #000000 }
        }
    }
}
