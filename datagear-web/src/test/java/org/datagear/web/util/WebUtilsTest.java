/*
 * Copyright 2018-2023 datagear.tech
 *
 * This file is part of DataGear.
 *
 * DataGear is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Lesser General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * DataGear is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with DataGear.
 * If not, see <https://www.gnu.org/licenses/>.
 */

package org.datagear.web.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * {@linkplain WebUtils}单元测试类。
 * 
 * @author datagear@163.com
 *
 */
public class WebUtilsTest
{
	@Test
	public void resolvePathAfterTest()
	{
		{
			String url = "/a/b/c";
			String re = WebUtils.resolvePathAfter(url, "/a");
			assertEquals("/b/c", re);
		}

		{
			String url = "/a/b/c#anchor";
			String re = WebUtils.resolvePathAfter(url, "/a");
			assertEquals("/b/c", re);
		}

		{
			String url = "/a/b/c?p=v";
			String re = WebUtils.resolvePathAfter(url, "/a");
			assertEquals("/b/c", re);
		}

		{
			String url = "/a/b/c;p=v";
			String re = WebUtils.resolvePathAfter(url, "/a");
			assertEquals("/b/c", re);
		}

		{
			String url = "/a/b/c;p=v#anchor";
			String re = WebUtils.resolvePathAfter(url, "/a");
			assertEquals("/b/c", re);
		}

		{
			String url = "/a/b/c?p=v#anchor";
			String re = WebUtils.resolvePathAfter(url, "/a");
			assertEquals("/b/c", re);
		}

		{
			String url = "/a/b/c;p=v?ppp=vvv";
			String re = WebUtils.resolvePathAfter(url, "/a");
			assertEquals("/b/c", re);
		}

		{
			String url = "/a/b/c;p=v?ppp=vvv#anchor";
			String re = WebUtils.resolvePathAfter(url, "/a");
			assertEquals("/b/c", re);
		}
	}

	@Test
	public void addUrlParamTest()
	{
		{
			String url = "/a/b";
			String re = WebUtils.addUrlParam(url, "p", "v");
			assertEquals("/a/b?p=v", re);
		}

		{
			String url = "/a/b?p0=v0";
			String re = WebUtils.addUrlParam(url, "p", "v");
			assertEquals("/a/b?p0=v0&p=v", re);
		}

		{
			String url = "/a/b#anchor";
			String re = WebUtils.addUrlParam(url, "p", "v");
			assertEquals("/a/b?p=v#anchor", re);
		}

		{
			String url = "/a/b?p0=v0#anchor";
			String re = WebUtils.addUrlParam(url, "p", "v");
			assertEquals("/a/b?p0=v0&p=v#anchor", re);
		}
	}
}
