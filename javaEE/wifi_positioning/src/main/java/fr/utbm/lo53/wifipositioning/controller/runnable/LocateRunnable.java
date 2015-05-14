package fr.utbm.lo53.wifipositioning.controller.runnable;

import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.utbm.lo53.wifipositioning.model.Measurement;
import fr.utbm.lo53.wifipositioning.model.Position;
import fr.utbm.lo53.wifipositioning.service.LocateService;

public class LocateRunnable extends SocketRunnable
{

	/** Logger of the class */
	private final static Logger	s_logger	= LoggerFactory.getLogger(LocateRunnable.class);

	private final LocateService	m_locateService;

	private final float			m_epsilon;

	public LocateRunnable(final Socket _clientSocket)
	{
		super(_clientSocket);

		m_locateService = LocateService.getInstance();

		m_packetOffset = Integer.parseInt(System.getProperty("locate.packet.offset"));

		m_epsilon = Float.parseFloat(System.getProperty("locate.rssi.epsilon"));

		m_runnableName = this.getClass().getSimpleName();
	}

	@Override
	protected void parseMobileRequestHandler() throws IOException
	{

		List<Measurement> measurements = new ArrayList<Measurement>();

		// String macAddress = "";
		// float x = -1.0f;
		// float y = -1.0f;
		// float rssi = -1.0f;

		byte bytes[] = IOUtils.toByteArray(m_clientSocket.getInputStream());

		try
		{
			List<Object> data = parseRequestData(bytes, m_packetOffset);
			if ((data == null) || data.isEmpty())
			{
				s_logger.error("Error, empty data list when parsing packet header.");
				handleResponse(m_clientSocket, "500".getBytes());
			}
			// macAddress = (String) data.get(0);
			// x = (float) data.get(1);
			// y = (float) data.get(2);
			// rssi = (float) data.get(3);

			Position queriedPosition = locate(measurements);

			if (queriedPosition == null)
			{
				s_logger.error("Error, queried position is null. No position have been queried.");
				handleResponse(m_clientSocket, "500".getBytes());
			} else
				handleResponse(m_clientSocket, ("x:" + queriedPosition.getX() + ";y:"
						+ queriedPosition.getY() + ";").getBytes());
		} finally
		{
			s_logger.debug("Locate response sent back to the client.");
		}
	}

	@Override
	protected List<Object> parseRequestData(
			final byte[] _bytes,
			final int _offset)
	{
		int offset = _offset;
		ArrayList<Object> list = new ArrayList<Object>();

		byte[] macAddressByteArray = Arrays.copyOfRange(_bytes, offset, offset
				+ m_macAddressByteLength);
		list.add(new String(macAddressByteArray));

		offset += m_macAddressByteLength;
		byte[] xByteArray = Arrays.copyOfRange(_bytes, offset, offset + m_positionByteLength);
		list.add(ByteBuffer.wrap(xByteArray).order(ByteOrder.LITTLE_ENDIAN).getFloat());

		offset += m_positionByteLength;
		byte[] yByteArray = Arrays.copyOfRange(_bytes, offset, offset + m_positionByteLength);
		list.add(ByteBuffer.wrap(yByteArray).order(ByteOrder.LITTLE_ENDIAN).getFloat());

		offset += m_positionByteLength;
		byte[] rssiByteArray = Arrays.copyOfRange(_bytes, offset, offset + m_rssiByteLength);
		list.add(ByteBuffer.wrap(rssiByteArray).order(ByteOrder.LITTLE_ENDIAN).getFloat());

		return list;
	}

	/**
	 * Allows you to
	 * 
	 * @param request
	 * @return "OK" if all the parameters are informed else it returns a
	 *         exception
	 * @throws IOException
	 * @throws IllegalArgumentException
	 */
	public Position locate(
			final List<Measurement> _measurements)
	{
		return m_locateService.queryPositionFromMeasurements(_measurements, m_epsilon);
	}
}