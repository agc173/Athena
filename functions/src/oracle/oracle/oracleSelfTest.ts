import {RequestType} from '../types';
import {validateOracleReading} from './schemas';

function runOracleSelfTest(): void {
  const valid = {
    type: RequestType.ORACLE_1Q,
    title: 'Energía para hoy',
    guidance: {
      core: 'Hoy avanza con calma y constancia.',
      do: ['Prioriza una tarea importante', 'Respira antes de responder'],
      avoid: ['Tomar decisiones impulsivas'],
      reflection: '¿Qué pequeño paso te da más paz ahora?',
    },
  };

  const invalid = {
    type: RequestType.ORACLE_1Q,
    title: ' ',
    guidance: {
      core: 'mensaje',
      do: ['solo uno'],
      avoid: [],
      reflection: 'reflexión',
    },
  };

  validateOracleReading(valid);

  let failedAsExpected = false;
  try {
    validateOracleReading(invalid as unknown as Record<string, unknown>);
  } catch {
    failedAsExpected = true;
  }

  if (!failedAsExpected) {
    throw new Error('Oracle self test failed: invalid object should be rejected');
  }
}

runOracleSelfTest();
